# 클릭 이상 감지 (급증 감지 + 봇 클릭 일일 요약)

## 개요

두 가지 클릭 이상 감지 기능을 추가한다.

1. **클릭 급증 감지** — 광고별 5분 윈도우 클릭수를 baseline(EMA)과 비교해 비정상 급증을 실시간 감지하고 조직에 알림 발송
2. **봇 클릭 일일 요약** — 전일 봇(의심) 클릭을 조직별로 집계해 매일 아침 요약 알림 발송

---

## 1. 클릭 급증 감지

### 처리 흐름

```
[클릭 이벤트] → Kafka (ad-click-events)
                    │
                    ▼
             ClickConsumer.consume()
               ├─ 분 단위 카운터 증가: click:real:{adId}:{yyyyMMddHHmm} (TTL 2h)
               └─ 활성 광고 등록: click:active:ads:{윈도우시작} Set에 "{adId}:{orgId}" 추가
                    │
                    ▼  (5분 주기, 매 5분 + 10초, 리더락)
             ClickSurgeScheduler
                    │
                    ▼
             ClickSurgeDetectionService.detectForWindow()
               ├─ 활성 Set 멤버별로 윈도우 클릭수 합산 (Redis multiGet)
               ├─ baseline 조회 → ClickSurgeCalculator.judge() 판정
               ├─ EMA baseline 갱신 (급증 윈도우는 제외 - baseline 오염 방지)
               ├─ 감지 시 click_anomaly_event 저장 (감사 이력)
               └─ streak + 쿨다운 통과 시 조직별 알림 발송
```

### 판정 알고리즘 (`ClickSurgeCalculator`)

순수 함수로 분리되어 단위 테스트가 용이하다.

1. **최소 절대 클릭수 가드** — 윈도우 클릭수 < `min-window-clicks`(30)이면 무조건 미감지 (저볼륨 광고의 노이즈성 배수 오탐 방지)
2. **cold-start** — baseline 전무(신규 광고 + 직전 1시간 트래픽 없음) 시 절대 기준(`cold-start-min-clicks`, 90) 단독 적용
3. **배수 기준** — `클릭수 / baseline 평균 >= multiplier-threshold`(3.0배)
4. **z-score 기준** — `(클릭수 - 평균) / σ_eff >= z-score-threshold`(3.5)
   - `σ_eff = max(EMA 표준편차, √평균, 1.0)` — Poisson 하한 + 절대 하한으로 저분산 슬롯의 과민 반응 억제

배수/z-score 중 하나라도 충족하면 감지. 판정 근거는 `SurgeDetectionBasis`(MULTIPLIER / Z_SCORE / BOTH / COLD_START)로 기록.

### Baseline 전략 (`BaselineSource`)

| 소스 | 조건 | 설명 |
|------|------|------|
| `EMA` | (요일, 시간대) 슬롯 샘플 수 >= `warmup-min-samples`(12) | `click_baseline_stat`의 EMA 평균/분산 사용. O(1) 갱신, 과거 데이터 재스캔 없음 |
| `ROLLING` | warm-up 미달 | 직전 1시간의 분 카운터(Redis)를 5분 윈도우로 합산해 표본 평균/표준편차 계산 |
| `NONE` | 롤링 데이터도 전무 | cold-start 절대 기준만 적용 |

- 슬롯 단위: 광고별 × 요일(1~7) × 시간대(0~23) — 요일·시간대별 트래픽 패턴 차이를 반영
- **급증 판정된 윈도우는 EMA에서 제외** — 이상값이 baseline을 끌어올려 다음 급증 감지를 희석시키는 것 방지

### 알림 발송 정책

- **streak** — `streak-required`(2)회 연속 감지 시에만 발송 (2회 = 10분 지속). 미감지 시 streak 즉시 리셋, 트래픽 끊기면 TTL(660초) 만료로 리셋
- **쿨다운** — 광고별 `cooldown-seconds`(1800초) 동안 재발송 방지 (Redis `SETNX` 선점)
- **조직 단위 묶음 발송** — 같은 조직의 여러 광고 급증을 알림 1건으로 병합
- **드라이런 모드** — `notify-enabled: false`이면 감지·기록만 하고 발송하지 않음 (임계값 튜닝용)
- 조직별 발송 실패는 격리되어 다른 조직 알림에 영향 없음

### 운영 안전장치

- **리더락** — `lock:scheduler:click-surge:{윈도우}` (TTL 290초). 다중 인스턴스에서 윈도우당 정확히 1회 처리
- **멱등성** — `uk_anomaly_ad_window` 유니크 제약으로 같은 (광고, 윈도우) 중복 이벤트 방지
- **+10초 오프셋** — 크론 `10 0/5 * * * *`로 Kafka 컨슈머 lag 정착 대기 후 직전 마감 윈도우 처리
- **타임존 고정** — 클릭 파이프라인 전체(`ClickConsumer` 적재, 스케줄러, 대시보드 조회, `clickedAt` 저장)가 `ClickWindowKeys.ZONE_ID`(Asia/Seoul)를 사용. 시스템 기본 존에 의존하지 않으므로 TZ 미설정 서버에서도 경계가 어긋나지 않음

---

## 2. 봇 클릭 일일 요약

### 처리 흐름

```
매일 09:00 KST (리더락: lock:scheduler:bot-summary:{yyyyMMdd}, TTL 1h)
    │
    ▼
BotClickDailySummaryScheduler
    │
    ▼
BotClickSummaryNotificationService.sendDailyBotSummaries()   ← 트랜잭션 밖 (NOT_SUPPORTED)
    │
    ├─ BotClickSummaryDataLoader.load(어제)                   ← readOnly 트랜잭션
    │    ├─ summarizeSuspectClicksByOrg: 조직별 [총 의심 클릭, 유니크 IP, 영향 광고 수]
    │    └─ summarizeSuspectAdClicksByOrg: 전 조직 광고별 집계 → 조직별 상위 3개 선별
    │
    └─ 조직별 sendApiAlarmToOrg() 발송 (실패 격리)
```

- 발송(외부 웹훅 호출)은 트랜잭션 밖에서 수행해 DB 커넥션을 점유하지 않음 (WeeklyReport 패턴)
- 봇 클릭 0건인 조직은 집계 결과에 포함되지 않아 자동 skip
- "어제" 경계는 KST 기준이며 `clickedAt` 저장 존과 동일

### 알림 메시지 예시

```
[조직명] 어제의 봇 클릭 요약 (8월 9일)
총 의심 클릭 152회 · 유니크 IP 23개 · 영향받은 광고 5개
상위 광고: 여름 세일 배너(87회), 신제품 런칭(41회), 브랜드 캠페인(12회)
```

---

## 설정 (`application.yml`)

```yaml
click:
  surge:
    enabled: true
    notify-enabled: true        # false = 감지·기록만 하는 드라이런 모드
    window-minutes: 5           # 60의 약수여야 함
    multiplier-threshold: 3.0
    z-score-threshold: 3.5
    min-window-clicks: 30
    cold-start-min-clicks: 90
    streak-required: 2
    cooldown-seconds: 1800
    ema-alpha: 0.2
    warmup-min-samples: 12
    active-set-ttl-seconds: 1800
  bot-summary:
    enabled: true
```

바인딩: `ClickSurgeProperties` (`@ConfigurationProperties(prefix = "click.surge")`)

---

## DB 스키마

### `click_baseline_stat` (신규)

광고별 (요일, 시간대) 슬롯의 5분 윈도우 클릭수 baseline.

| 컬럼 | 설명 |
|------|------|
| `ad_content_id` | 광고 ID |
| `weekday` | 1(월) ~ 7(일) |
| `hour_of_day` | 0 ~ 23 |
| `ema_mean` / `ema_variance` | EMA 평균 / 분산 |
| `sample_count` | 누적 샘플 수 (warm-up 판단용) |

유니크: `uk_baseline_ad_slot (ad_content_id, weekday, hour_of_day)`

### `click_anomaly_event` (신규)

급증 감지 이력 (감사 로그 + 추후 대시보드 "이상 이력" 조회용).

| 컬럼 | 설명 |
|------|------|
| `ad_content_id` / `org_id` | 대상 광고 / 조직 |
| `window_start` / `window_clicks` | 윈도우 시작 / 클릭수 |
| `baseline_mean` / `baseline_std` | 판정 당시 baseline (NONE이면 null) |
| `z_score` / `multiplier_ratio` | 판정 수치 |
| `detection_basis` | MULTIPLIER / Z_SCORE / BOTH / COLD_START |
| `baseline_source` | EMA / ROLLING / NONE |
| `notified` | 실제 알림 발송 여부 |

유니크: `uk_anomaly_ad_window (ad_content_id, window_start)` / 인덱스: `idx_anomaly_org_created (org_id, created_at)`

### `click_log` (변경)

일일 요약 집계용 인덱스 추가: `idx_click_log_suspect_clicked (is_suspect, clicked_at)`

> **⚠️ 배포 체크리스트**: `ddl-auto: update`는 기존 테이블에 인덱스를 추가하지 않는다. 운영 DB에 수동 실행 필요:
> ```sql
> CREATE INDEX idx_click_log_suspect_clicked ON click_log (is_suspect, clicked_at);
> ```

---

## Redis 키

| 키 | 용도 | TTL |
|----|------|-----|
| `click:real:{adId}:{yyyyMMddHHmm}` | 분 단위 클릭 카운터 (기존) | 2h |
| `click:active:ads:{윈도우시작}` | 윈도우별 활성 광고 Set (`{adId}:{orgId}`) | 30m |
| `click:surge:streak:{adId}` | 연속 감지 카운터 | 660s |
| `notification:cooldown:surge:ad:{adId}` | 광고별 알림 쿨다운 | 30m |
| `lock:scheduler:click-surge:{윈도우}` | 급증 감지 리더락 | 290s |
| `lock:scheduler:bot-summary:{yyyyMMdd}` | 일일 요약 리더락 | 1h |

키 포맷은 `ClickWindowKeys`에 집중되어 있으며, 적재(`ClickConsumer`)와 조회(`ClickSurgeDetectionService`)가 반드시 같은 포맷·타임존을 사용해야 한다.

---

## 주요 파일

### 급증 감지 (click 도메인)

| 파일 | 역할 |
|------|------|
| `domain/config/ClickSurgeProperties.java` | 설정 바인딩 |
| `domain/constant/ClickWindowKeys.java` | Redis 키 포맷 + 공통 타임존(`ZONE_ID`) |
| `domain/constant/SurgeDetectionBasis.java` | 판정 근거 enum |
| `domain/constant/BaselineSource.java` | baseline 소스 enum |
| `domain/service/ClickSurgeCalculator.java` | 판정·EMA 갱신 순수 함수 |
| `domain/service/ClickSurgeDetectionService.java` | 윈도우 처리 오케스트레이션 |
| `domain/service/scheduler/ClickSurgeScheduler.java` | 5분 주기 스케줄러 + 리더락 |
| `persistence/entity/ClickBaselineStat.java` | baseline 엔티티 |
| `persistence/entity/ClickAnomalyEvent.java` | 감지 이력 엔티티 |
| `persistence/repository/ClickBaselineStatRepository.java` | 슬롯 배치 IN 조회 |
| `persistence/repository/ClickAnomalyEventRepository.java` | 이력 저장 |

### 봇 클릭 일일 요약 (notification 도메인)

| 파일 | 역할 |
|------|------|
| `application/dto/BotClickSummaryData.java` | 조직별 요약 DTO |
| `domain/service/BotClickSummaryDataLoader.java` | 집계 조회 (readOnly 트랜잭션) |
| `domain/service/BotClickSummaryNotificationService.java` | 메시지 구성·발송 (트랜잭션 밖) |
| `domain/service/scheduler/BotClickDailySummaryScheduler.java` | 매일 09:00 KST 스케줄러 + 리더락 |

### 기존 파일 변경

| 파일 | 변경 |
|------|------|
| `ClickConsumer` | 활성 광고 Set 등록 추가, 타임존 KST 고정 |
| `ClickLogRepository` | 조직별 의심 클릭 집계 쿼리 2종 추가 |
| `ClickLog` | 집계용 인덱스 추가 |
| `ClickConverter` | `clickedAt` 저장 존 KST 고정 |
| `ClickServiceImpl` | 실시간 조회 타임존 KST 통일 |
| `RedisUtil` | `sAddExpire`, `sMembers`, `multiGetData` 헬퍼 추가 |

### 테스트

- `ClickSurgeCalculatorTest` — 판정 로직 단위 테스트
- `ClickSurgeDetectionServiceTest` — 윈도우 처리 흐름 테스트
- `BotClickSummaryNotificationServiceTest` — 요약 발송 테스트

---

## 로컬 E2E 테스트 검증 (2026-08-10)

디스코드 웹훅 연동 후 실제 클릭 → 감지 → 알림 전체 파이프라인을 검증했다.

### 테스트용 임계값

운영 기본값으로는 90클릭 × 연속 2윈도우가 필요해 수동 테스트가 어려우므로 임시로 낮춰서 진행:

| 설정 | 운영값 | 테스트값 |
|------|--------|----------|
| `min-window-clicks` | 30 | 1 |
| `cold-start-min-clicks` | 90 | 10 |
| `streak-required` | 2 | 1 |

### 절차

1. 디스코드 웹훅 등록: `PATCH /api/notification/settings/{orgId}/org` (`isDiscordEnabled`, `discordWebhookUrl`, `alertClicks`)
2. 연동 확인: `POST /api/notification/org/{orgId}/test`
3. 트래킹 URL 발급: `POST /api/clicks/{orgId}/{adContentId}/tracking-url`
4. 한 5분 윈도우 안에서 트래킹 URL 20회+ 클릭
5. 윈도우 마감 후 +10초에 스케줄러 감지 → 디스코드 알림 수신

### 실제 수신 결과

```
[where you ad 1] 비정상 클릭 감지
비정상적인 트래픽(Bot)이 감지되었습니다. (IP: 0:0:0:0:0:0:0:1)

클릭 급증 감지 (광고 1건)
알 수 없는 광고: 최근 5분 22회 클릭 (평소 0.3회 대비 22.0배)
```

- 첫 번째: 기존 봇 클릭 즉시 알림 (클릭 단위, ClickConsumer 경로)
- 두 번째: 신규 급증 감지 알림 (윈도우 단위 배치). 직전 윈도우의 소량 클릭이 롤링 baseline(평균 0.3회)이 되어 `MULTIPLIER`(22.0배) 근거로 감지됨

### 확인된 동작 특성

- **알림 지연은 최소 10초 ~ 최대 5분 10초**: 클릭 시점이 아니라 윈도우 마감 후 스케줄러 실행 시점에 발송된다 (클릭 단위 실시간 판정이 아님)
- 클릭이 윈도우 경계에 걸쳐 쪼개지면 각각 기준 미달이 될 수 있으므로, 테스트 시 윈도우 초반에 몰아서 클릭해야 확실하다
- baseline이 생기기 전 첫 감지는 `COLD_START`(절대 기준), 이후에는 직전 트래픽 기반 `ROLLING` + `MULTIPLIER`로 감지된다
- 광고명이 null이면 "알 수 없는 광고"로 표기된다

> 테스트 후 임계값을 운영값으로 원복하고, `click_anomaly_event`·`click_baseline_stat` 테스트 데이터와 Redis 쿨다운 키(`notification:cooldown:surge:ad:{adId}`)를 정리할 것.
