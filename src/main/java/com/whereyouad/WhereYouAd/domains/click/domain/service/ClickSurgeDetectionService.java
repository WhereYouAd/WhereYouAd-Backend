package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.domain.config.ClickSurgeProperties;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.BaselineSource;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickSurgeCalculator.BaselineSnapshot;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickSurgeCalculator.SurgeVerdict;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickAnomalyEvent;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickBaselineStat;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickAnomalyEventRepository;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickBaselineStatRepository;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationService;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 5분 윈도우 단위 클릭 급증 감지 오케스트레이션.
 * 흐름: 활성 광고 조회(Redis Set) → 클릭수 합산 → baseline 비교 판정(Calculator)
 *      → baseline 갱신 → 이력 저장 → streak/쿨다운 통과 시 조직별 알림.
 * ClickSurgeScheduler가 리더락을 잡고 윈도우당 1회 호출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClickSurgeDetectionService {

    private final RedisUtil redisUtil;
    private final ClickBaselineStatRepository baselineStatRepository;
    private final ClickAnomalyEventRepository anomalyEventRepository;
    private final AdContentRepository adContentRepository;
    private final NotificationService notificationService;
    private final ClickSurgeProperties properties;
    private final PlatformTransactionManager transactionManager;

    private static final String STREAK_KEY_PREFIX = "click:surge:streak:";
    private static final String COOLDOWN_KEY_PREFIX = "notification:cooldown:surge:ad:";
    // 윈도우 2개 + 여유. TTL은 상태 정리용이며 연속성은 저장된 마지막 감지 윈도우로 판단한다.
    private static final long STREAK_TTL_SECONDS = 660;

    // 알림 발송 확정된 감지 건 1개 (조직별로 모아서 알림 1건으로 병합). event는 중복 저장 스킵 시 null
    private record CooldownClaim(String key, String ownershipToken) {
    }

    private record SurgeAlert(Long adContentId, long clicks, BaselineSnapshot baseline, SurgeVerdict verdict,
                              ClickAnomalyEvent event, CooldownClaim cooldownClaim) {
    }

    public void detectForWindow(LocalDateTime windowStart) {
        // 1. 이번 윈도우에 클릭이 있었던 광고 목록(출석부) 조회 - 없으면 검사할 것도 없음
        Set<String> members = redisUtil.sMembers(ClickWindowKeys.activeAdsKey(windowStart));
        if (members.isEmpty()) {
            return;
        }

        // 2. "{adId}:{orgId}" 문자열을 Map으로 파싱
        Map<Long, Long> orgIdByAdId = parseMembers(members);
        if (orgIdByAdId.isEmpty()) {
            return;
        }

        // 3~5. baseline 조회 → 판정 → EMA 갱신·저장을 한 트랜잭션으로 처리
        //      (조회 엔티티를 managed 상태로 유지해 saveAll 시 엔티티별 merge SELECT 제거)
        Map<Long, List<SurgeAlert>> alertsByOrg = new TransactionTemplate(transactionManager)
                .execute(status -> runDetection(orgIdByAdId, windowStart));

        // 6. 알림 발송은 트랜잭션 밖 - 외부 호출이 DB 커넥션을 점유하지 않도록
        if (alertsByOrg != null && !alertsByOrg.isEmpty()) {
            notifyByOrg(alertsByOrg);
        }
    }

    private Map<Long, List<SurgeAlert>> runDetection(Map<Long, Long> orgIdByAdId, LocalDateTime windowStart) {
        // 3. 활성 광고 전체의 (요일, 시간대) 슬롯 baseline을 쿼리 1방에 일괄 조회 (N+1 방지)
        int weekday = windowStart.getDayOfWeek().getValue();
        int hourOfDay = windowStart.getHour();

        Map<Long, ClickBaselineStat> statsByAdId = baselineStatRepository
                .findByAdContentIdInAndWeekdayAndHourOfDay(orgIdByAdId.keySet(), weekday, hourOfDay)
                .stream()
                .collect(Collectors.toMap(ClickBaselineStat::getAdContentId, Function.identity()));

        List<ClickBaselineStat> statsToSave = new ArrayList<>();
        Map<Long, List<SurgeAlert>> alertsByOrg = new HashMap<>();
        // 조직별 수신 가능 채널 존재 여부 캐시 - 같은 조직 광고마다 설정을 중복 조회하지 않도록
        Map<Long, Boolean> alarmActiveByOrg = new HashMap<>();

        // 4. 광고별 판정 루프
        for (Map.Entry<Long, Long> entry : orgIdByAdId.entrySet()) {
            Long adContentId = entry.getKey();
            Long orgId = entry.getValue();

            long clicks = sumWindowClicks(adContentId, windowStart);
            ClickBaselineStat stat = statsByAdId.get(adContentId);
            BaselineSnapshot baseline = resolveBaseline(stat, adContentId, windowStart);
            SurgeVerdict verdict = ClickSurgeCalculator.judge(clicks, baseline, properties);

            // 첫 등장 광고는 슬롯 행 신규 생성
            if (stat == null) {
                stat = ClickBaselineStat.init(adContentId, weekday, hourOfDay);
            }
            // 급증 윈도우는 EMA에서 제외 - 이상값이 baseline을 끌어올려 다음 급증을 희석시키는 것 방지
            if (!verdict.detected()) {
                ClickSurgeCalculator.applyEma(stat, clicks, properties.getEmaAlpha());
            }
            statsToSave.add(stat);

            if (verdict.detected()) {
                // 감지 즉시 이력은 무조건 저장(notified=false). 발송 성공 후에만 notified=true로 갱신
                int streak = incrementStreak(adContentId, windowStart);
                ClickAnomalyEvent savedEvent = saveAnomalyEvent(adContentId, orgId, windowStart, clicks, baseline, verdict);
                // 알림 비활성(드라이런) 모드나 클릭 알림을 꺼둔 조직은 쿨다운을 소모하지 않는다
                // (쿨다운만 선점하고 발송은 skip되면, 그 사이 알림을 켠 조직이 다음 급증 알림을 놓치게 됨)
                CooldownClaim cooldownClaim = null;
                if (properties.isNotifyEnabled() && streak >= properties.getStreakRequired()
                        && alarmActiveByOrg.computeIfAbsent(orgId,
                                key -> notificationService.isAnyAlarmActive(key, NotificationType.CLICKS_INCREASE))) {
                    cooldownClaim = acquireCooldown(adContentId);
                }
                if (cooldownClaim != null) {
                    alertsByOrg.computeIfAbsent(orgId, key -> new ArrayList<>())
                            .add(new SurgeAlert(adContentId, clicks, baseline, verdict, savedEvent, cooldownClaim));
                }
            } else {
                // 미감지 = 연속성 끊김 → streak 리셋
                redisUtil.deleteData(STREAK_KEY_PREFIX + adContentId);
            }
        }

        // 5. baseline 일괄 저장 (기존 엔티티는 managed 상태라 dirty checking, 신규 엔티티만 persist)
        baselineStatRepository.saveAll(statsToSave);
        return alertsByOrg;
    }

    // active set 멤버 "{adId}:{orgId}" → Map<adId, orgId>. 형식이 깨진 멤버는 로그만 남기고 skip
    private Map<Long, Long> parseMembers(Set<String> members) {
        Map<Long, Long> orgIdByAdId = new HashMap<>();
        for (String member : members) {
            String[] parts = member.split(":");
            if (parts.length != 2) {
                log.warn("[급증감지] 잘못된 active set 멤버 형식: {}", member);
                continue;
            }
            try {
                orgIdByAdId.put(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
            } catch (NumberFormatException e) {
                log.warn("[급증감지] 잘못된 active set 멤버 값: {}", member);
            }
        }
        return orgIdByAdId;
    }

    // 윈도우 내 분 단위 카운터(click:real:{adId}:{분}) 5개를 multiGet으로 한 번에 읽어 합산
    private long sumWindowClicks(Long adContentId, LocalDateTime windowStart) {
        List<String> keys = new ArrayList<>(properties.getWindowMinutes());
        for (int i = 0; i < properties.getWindowMinutes(); i++) {
            keys.add(ClickWindowKeys.realMinuteKey(adContentId, windowStart.plusMinutes(i)));
        }
        return redisUtil.multiGetData(keys).stream()
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::parseLong)
                .sum();
    }

    // baseline 선택: EMA(샘플 충분) → 롤링(직전 1시간) → NONE 순서로 폴백
    private BaselineSnapshot resolveBaseline(ClickBaselineStat stat, Long adContentId, LocalDateTime windowStart) {
        if (stat != null && stat.getSampleCount() >= properties.getWarmupMinSamples()) {
            return ClickSurgeCalculator.fromEma(stat);
        }

        // warm-up 미달 - 직전 1시간 분 카운터(Redis TTL 2시간 내)를 5분 윈도우 단위로 합산해 롤링 baseline 구성
        int windowMinutes = properties.getWindowMinutes();
        int rollingWindowCount = 60 / windowMinutes;
        List<String> keys = new ArrayList<>(60);
        LocalDateTime rollingStart = windowStart.minusMinutes(60);
        for (int i = 0; i < 60; i++) {
            keys.add(ClickWindowKeys.realMinuteKey(adContentId, rollingStart.plusMinutes(i)));
        }
        List<String> values = redisUtil.multiGetData(keys);

        List<Long> windowSums = new ArrayList<>(rollingWindowCount);
        for (int w = 0; w < rollingWindowCount; w++) {
            long sum = 0;
            for (int m = 0; m < windowMinutes; m++) {
                int index = w * windowMinutes + m;
                if (index < values.size() && values.get(index) != null) {
                    sum += Long.parseLong(values.get(index));
                }
            }
            windowSums.add(sum);
        }
        return ClickSurgeCalculator.fromRollingWindows(windowSums);
    }

    // 마지막 감지 윈도우를 함께 저장해, 중간에 비어 있던 윈도우도 다음 감지 시 리셋한다.
    private int incrementStreak(Long adContentId, LocalDateTime windowStart) {
        String streakKey = STREAK_KEY_PREFIX + adContentId;
        String currentWindow = windowStart.format(ClickWindowKeys.MINUTE_FORMATTER);
        String previousWindow = windowStart.minusMinutes(properties.getWindowMinutes())
                .format(ClickWindowKeys.MINUTE_FORMATTER);
        Long streak = redisUtil.advanceClickSurgeStreak(
                streakKey, currentWindow, previousWindow, STREAK_TTL_SECONDS);
        if (streak == null) {
            throw new IllegalStateException("Unable to advance click surge streak");
        }
        return Math.toIntExact(streak);
    }

    // 쿨다운 선점 성공 시에만 발송 (TTL 동안 같은 광고 재발송 방지)
    private CooldownClaim acquireCooldown(Long adContentId) {
        String key = COOLDOWN_KEY_PREFIX + adContentId;
        String ownershipToken = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisUtil.setIfAbsent(key, ownershipToken, properties.getCooldownSeconds()))) {
            return null;
        }
        return new CooldownClaim(key, ownershipToken);
    }

    // 저장 성공 시 엔티티 반환, 유니크 충돌(중복 실행)이면 null.
    // REQUIRES_NEW 분리 - 유니크 충돌 catch가 바깥 감지 트랜잭션을 rollback-only로 만들지 않도록
    private ClickAnomalyEvent saveAnomalyEvent(Long adContentId, Long orgId, LocalDateTime windowStart, long clicks,
                                               BaselineSnapshot baseline, SurgeVerdict verdict) {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            return requiresNew.execute(status -> anomalyEventRepository.save(ClickAnomalyEvent.builder()
                    .adContentId(adContentId)
                    .orgId(orgId)
                    .windowStart(windowStart)
                    .windowClicks((int) clicks)
                    .baselineMean(baseline.source() != BaselineSource.NONE ? baseline.mean() : null)
                    .baselineStd(baseline.source() != BaselineSource.NONE ? baseline.std() : null)
                    .zScore(verdict.zScore())
                    .multiplierRatio(verdict.ratio())
                    .detectionBasis(verdict.basis())
                    .baselineSource(baseline.source())
                    .notified(false)
                    .build()));
        } catch (DataIntegrityViolationException e) {
            // uk_anomaly_ad_window - 같은 윈도우 중복 실행 시 멱등 처리
            log.debug("[급증감지] 중복 이벤트 스킵: adId={}, windowStart={}", adContentId, windowStart);
            return null;
        }
    }

    // 같은 조직의 여러 광고 급증을 알림 1건으로 병합 발송 (알림 스팸 방지)
    private void notifyByOrg(Map<Long, List<SurgeAlert>> alertsByOrg) {
        // 알림 문구용 광고명을 일괄 조회
        List<Long> adContentIds = alertsByOrg.values().stream()
                .flatMap(List::stream)
                .map(SurgeAlert::adContentId)
                .toList();
        Map<Long, String> adNameById = adContentRepository.findAllById(adContentIds).stream()
                .collect(Collectors.toMap(AdContent::getId, ad -> ad.getName() != null ? ad.getName() : "알 수 없는 광고"));

        for (Map.Entry<Long, List<SurgeAlert>> entry : alertsByOrg.entrySet()) {
            Long orgId = entry.getKey();
            List<SurgeAlert> alerts = entry.getValue();
            try {
                String title = String.format("클릭 급증 감지 (광고 %d건)", alerts.size());
                String message = alerts.stream()
                        .map(alert -> formatAlertLine(alert, adNameById))
                        .collect(Collectors.joining("\n"));
                notificationService.sendApiAlarmToOrg(orgId, NotificationType.CLICKS_INCREASE, title, message);
                notificationService.sendBrowserPushToOrg(orgId, NotificationType.CLICKS_INCREASE, title, message, null);
            } catch (Exception e) {
                // 조직별 발송 실패 격리 - 다른 조직 알림에 영향 없도록. 실패 시 notified=false 유지
                releaseCooldowns(alerts);
                log.error("[급증감지] 알림 발송 실패: orgId={}", orgId, e);
                continue;
            }
            try {
                markEventsNotified(alerts);
            } catch (Exception e) {
                // 실제 발송은 성공했으므로 중복 알림 방지를 위해 쿨다운은 유지한다.
                log.error("[급증감지] 알림 발송 후 이벤트 갱신 실패: orgId={}", orgId, e);
            }
        }
    }

    private void releaseCooldowns(List<SurgeAlert> alerts) {
        for (SurgeAlert alert : alerts) {
            CooldownClaim cooldownClaim = alert.cooldownClaim();
            try {
                redisUtil.deleteIfValueMatches(cooldownClaim.key(), cooldownClaim.ownershipToken());
            } catch (Exception e) {
                log.error("[급증감지] 실패한 알림의 쿨다운 해제 실패: adContentId={}", alert.adContentId(), e);
            }
        }
    }

    // 발송 성공한 조직의 이벤트만 notified=true로 갱신
    private void markEventsNotified(List<SurgeAlert> alerts) {
        List<ClickAnomalyEvent> events = alerts.stream()
                .map(SurgeAlert::event)
                .filter(java.util.Objects::nonNull)
                .toList();
        events.forEach(ClickAnomalyEvent::markNotified);
        anomalyEventRepository.saveAll(events);
    }

    // 알림 본문 한 줄 생성. baseline 유무에 따라 문구 분기 (신규 광고는 "평소 대비"를 쓸 수 없음)
    private String formatAlertLine(SurgeAlert alert, Map<Long, String> adNameById) {
        String adName = adNameById.getOrDefault(alert.adContentId(), "광고 ID " + alert.adContentId());
        if (alert.baseline().source() == BaselineSource.NONE) {
            return String.format("%s: 최근 %d분 %d회 클릭 (신규 광고 - 절대 기준 초과)",
                    adName, properties.getWindowMinutes(), alert.clicks());
        }
        return String.format("%s: 최근 %d분 %d회 클릭 (평소 %.1f회 대비 %.1f배)",
                adName, properties.getWindowMinutes(), alert.clicks(),
                alert.baseline().mean(), alert.verdict().ratio());
    }
}
