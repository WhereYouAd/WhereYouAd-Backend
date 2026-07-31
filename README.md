<div align="center">

2026 상명대학교 졸업 프로젝트

**분산된 광고 데이터를 하나의 대시보드로 광고 성과를 통합하고 AI가 분석해 드립니다**

<img width="3145" height="1769" alt="wyad" src="https://github.com/user-attachments/assets/9c284495-258e-4fd1-97f7-027a5034774f" />

![Java][java]
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2024.0.1-6DB33F?logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-3.7.0-231F20?logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_+_AWS_EC2-2496ED?logo=docker&logoColor=white)

🏆 상명대학교 교내 창업아이디어 경진대회 **대상**

🏆 모두의 창업 공모전 **1기 선정 · 본선 진출**

</div>

<br>

## 📌 Overview

WhereYouAd는 Google, Naver, Meta 3개 광고 플랫폼의 성과 데이터를 단일 대시보드에 통합하는 B2B SaaS입니다. 팀 단위 워크스페이스로 멤버가 협업하며, AI 분석 리포트를 생성하고 PDF로 저장할 수 있습니다.

WhereYouAd Backend는 Spring Boot 기반의 2026 캡스톤 졸업 프로젝트입니다. 서로 다른 인증 방식과 응답 스키마를 가진 3개 광고 플랫폼 API를 하나의 도메인 모델로 정규화하고, Kafka 기반 클릭 이벤트 파이프라인·SSE 실시간 전송·OpenAI 리포트 생성까지 도메인 계층형 아키텍처 위에서 직접 설계·구현합니다.

<br>

### 핵심 기능 영역

- Google Ads / Meta Marketing / Naver Search Ad API 통합 및 지표 정규화
- OAuth2 소셜 로그인(Naver·Google·Kakao) + JWT 인증/재발급
- Kafka 기반 클릭 이벤트 수집 · 봇 필터링 · SSE 실시간 전송
- 광고 트래킹 링크 발급 (shortURL 코드 + 리다이렉트 수집)
- OpenAI 기반 AI 분석 리포트 비동기 생성 및 이메일 발송
- 조직(워크스페이스) · 멤버 초대 · RBAC 권한 관리
- 기간 비교 기반 타임라인 성과 분석
- 주간 리포트 스케줄러 + Discord / Slack Webhook 알림

<br>

## 📑 Table of Contents

- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Scripts](#-scripts)
- [Project Structure](#-project-structure)
- [CI/CD](#-ci--cd)
- [Conventions](#-conventions)
- [Contributors](#-contributors)

<br>

## 🛠 Tech Stack

| Category      | Stack                                                                      |
| ------------- | -------------------------------------------------------------------------- |
| Core          | Java 17, Spring Boot 3.5.9, Spring Cloud 2024.0.1, Gradle                  |
| Security      | Spring Security, OAuth2 Client (Naver·Google·Kakao), JWT(jjwt 0.11.5), AES |
| Persistence   | MySQL 8.0, Spring Data JPA (Hibernate)                                     |
| Cache         | Redis (RefreshToken · 인증 코드 · 토큰 캐시)                               |
| Messaging     | Apache Kafka 3.7.0 (클릭 이벤트 Producer / Consumer)                       |
| Realtime      | SSE (`SseEmitter` + `SseEmitterRepository`)                                |
| HTTP Client   | OpenFeign (OpenAI), WebClient (Google·Meta·Naver Ads)                      |
| Ad Platform   | Google Ads API 42.0.0, Meta Marketing API, Naver Search Ad API             |
| AI            | OpenAI API (분석 리포트 · 예산 추천)                                       |
| Notification  | Gmail SMTP + Thymeleaf, Discord / Slack Webhook, CoolSMS(nurigo 4.3.0)     |
| Storage       | AWS S3 (Spring Cloud AWS 3.4.2)                                            |
| Resilience    | Spring Retry + Spring AOP (외부 API 재시도)                                |
| Scheduling    | Spring Scheduler (플랫폼 동기화 · 주간 리포트 · 계정 정리)                 |
| Docs          | Springdoc OpenAPI 2.8.0 (Swagger UI)                                       |
| Config        | Spring Dotenv 4.0.0 (`.env` 로딩)                                          |
| Test          | JUnit 5, Spring Boot Test, Spring Security Test                            |
| Deploy        | Docker, Docker Compose, Docker Hub, AWS EC2 (Bastion 경유 배포)           |

<br>

## 🚀 Getting Started

### Prerequisites

```bash
java -version
# openjdk version "17.x.x"
```

Docker / Docker Compose가 설치되어 있어야 합니다. (MySQL · Redis · Kafka 로컬 구동)

### Installation

```bash
cp .env.example .env
```

### Run Local Server

```bash
./gradlew bootRun
```

### Run Full Stack

```bash
docker-compose up -d
```

### Build

```bash
./gradlew clean build -x test
```

앱이 뜨면 `http://localhost:8080/swagger-ui/index.html` 에서 API 문서를 확인할 수 있습니다.

<br>

## 📜 Scripts

| Command                         | Description                                       |
| ------------------------------- | ------------------------------------------------- |
| `./gradlew build`               | 테스트를 포함한 전체 빌드를 수행합니다.           |
| `./gradlew clean build -x test` | 테스트를 제외하고 빌드합니다. (CD에서 사용)       |
| `./gradlew test`                | 테스트만 실행합니다. (MySQL + Redis 필요)         |
| `./gradlew bootRun`             | 로컬 서버를 실행합니다. (`.env` 필요)             |
| `docker-compose up -d`          | App + MySQL + Redis + Kafka 전체 스택을 띄웁니다. |
| `docker-compose logs -f app`    | 애플리케이션 컨테이너 로그를 확인합니다.          |

<br>

## 📁 Project Structure

```
src/main/java/com/whereyouad/WhereYouAd
├── domains                    # 핵심 비즈니스 도메인 (계층형 패키지)
│   ├── user/                  # 회원, 소셜 로그인, 계정 찾기·비밀번호 재설정
│   ├── organization/          # 워크스페이스, 멤버 초대, OrgRole(RBAC)
│   ├── project/               # 프로젝트 단위 광고 캠페인 그룹
│   ├── platform/              # 플랫폼 계정·연동 상태, 동기화 스케줄러
│   ├── advertisement/         # 캠페인·광고그룹·소재·예산·MetricFact
│   │   └── domain/service/adapi/{google,meta}  # 플랫폼별 연동 서비스
│   ├── dashboard/             # 통합 KPI 집계, 클릭 대시보드
│   ├── timeline/              # 타임라인 CRUD 및 비교 성과 분석 
│   ├── click/                 # 트래킹 URL, BotDetector, ClickEventProducer
│   ├── ai/                    # OpenAI 리포트 비동기 생성 (AIStatus)
│   ├── notification/          # 알림 설정, 주간 리포트 스케줄러, 발송 이력
│   └── image/                 # 이미지 업로드
│
│   └── {domain}/              # 공통 계층 구조
│       ├── application/       # dto/request · dto/response · mapper
│       ├── domain/            # service (비즈니스 로직) · constant (Enum)
│       ├── persistence/       # entity (JPA) · repository
│       ├── presentation/      # Controller + docs (Swagger 인터페이스 분리)
│       └── exception/         # Handler + code (BaseErrorCode 구현체)
│
├── global                     # 전역 인프라 레이어
│   ├── security/
│   │   ├── jwt/               # JwtTokenProvider, JwtAuthenticationFilter
│   │   ├── oauth2/            # Naver·Google·Kakao Response 추상화 + SuccessHandler
│   │   ├── cookie/            # AuthCookieFactory (SameSite·Secure 제어)
│   │   └── SecurityConfig.java
│   ├── adapi/                 # AdAuthFactory + AdAuthStrategy (플랫폼 인증 추상화)
│   ├── config/                # Swagger, Redis, WebClient, OpenAIFeign, Retry
│   ├── response/              # BaseResponse, DataResponse<T>, ErrorResponse
│   ├── exception/             # 전역 예외 핸들러 (BaseErrorCode → HTTP 매핑)
│   ├── sse/                   # SseEmitterRepository (실시간 클릭 스트림)
│   ├── scheduler/             # 공용 스케줄 설정
│   ├── common/                # BaseEntity (Auditing)
│   └── utils/                 # AESUtil, RedisUtil, CursorUtil, BudgetCalculator, MetricCalculator
│
└── infrastructure/client      # 외부 시스템 연동 클라이언트
    ├── google/                # GoogleAdWebClient + converter/dto
    ├── meta/                  # Meta Marketing API client/config/converter/dto
    ├── naver/                 # Naver Search Ad API client/converter/dto
    ├── openai/                # Feign client + prompt + service
    ├── kafka/                 # KafkaClickEventProducer, ClickConsumer
    ├── aws/s3/                # S3 이미지 업로드
    ├── mail/                  # AIMailService (Thymeleaf 템플릿)
    ├── discord/               # DiscordWebhookClient
    └── slack/                 # SlackWebhookClient
```

<br>

## ⚙️ CI / CD

| 워크플로  | 트리거                     | 내용                                                                        |
| --------- | -------------------------- | --------------------------------------------------------------------------- |
| `ci.yml`  | push / PR → develop, main | MySQL·Redis 서비스 컨테이너 기동 → JDK 17 셋업 → `./gradlew build` 검증     |
| `cd.yml`  | push → develop            | `clean build -x test` → Docker 이미지 빌드·Hub 푸시 → Bastion 경유 EC2 배포 |

배포 서버에서는 `docker-compose pull && up -d`로 컨테이너를 교체하고, `docker image prune -f`로 잔여 이미지를 정리합니다.

<br>

## 🤝 Conventions

### Branch

- `feat/#1-description`
- `fix/#1-description`
- `refactor/#1-description`
- `deploy/#1-description`
- `chore/#1-description`

`develop`이 배포 기준 브랜치이며, 모든 작업은 이슈 번호 기반 브랜치에서 진행 후 PR로 병합합니다.

### Commit

Angular Commit Convention에 Gitmoji를 결합한 `:gitmoji: type: subject` 형식을 따릅니다.

| Gitmoji                 | Type       | 설명                                            |
| ----------------------- | ---------- | ----------------------------------------------- |
| ✨ `:sparkles:`          | `feat`     | 새로운 기능 추가                                |
| 🐛 `:bug:`              | `fix`      | 버그 수정                                       |
| 📝 `:memo:`             | `docs`     | 문서 수정 (README, Swagger Docs 등)             |
| 🎨 `:art:`              | `style`    | 포맷팅·공백 등 (동작 변경 없음)                 |
| ♻️ `:recycle:`          | `refactor` | 코드 리팩토링 (기능 변화 없음)                  |
| ✅ `:white_check_mark:` | `test`     | 테스트 코드 추가/수정                           |
| 🔧 `:wrench:`           | `chore`    | 빌드·설정·의존성 등 기타 변경                   |
| 💚 `:green_heart:`      | `ci`       | CI 설정 변경                                    |
| 🚀 `:rocket:`           | `deploy`   | 배포 관련 수정 (Dockerfile, docker-compose 등) |

### Pull Request

- PR 제목은 `[Type/#이슈번호] 작업 요약` 형식을 사용합니다. (예: `[Feat/#1] 로그인 API 구현`)
- GitHub Issue를 먼저 등록하고 PR 본문에 `closes #이슈번호`를 포함합니다.
- 리뷰 라벨로 반영 우선순위를 구분합니다.

| Label | 의미                       | 처리                 |
| ----- | -------------------------- | -------------------- |
| `P1`  | 필수 반영 (Critical)       | 반영 전 머지 불가    |
| `P2`  | 적극 권장 (Recommended)    | 반영 권장            |
| `P3`  | 제안 (Suggestion)          | 작성자 자율          |
| `P4`  | 단순 확인 (Nit)            | 오타·칭찬 등         |

### Code Quality

- 모든 API 응답은 `BaseResponse` 계열 래퍼(`DataResponse<T>`, `ErrorResponse`)로 통일합니다.
- 에러는 도메인별 `BaseErrorCode` 구현체로 정의하고, 도메인 예외를 던져 전역 핸들러가 HTTP로 매핑합니다.
- Swagger 어노테이션은 `presentation/docs/` 인터페이스로 분리해 Controller 코드를 깨끗하게 유지합니다.
- 외부 광고 API 인증은 `AdAuthFactory` + `AdAuthStrategy`로 추상화해 플랫폼 추가 시 전략만 확장합니다.
- 외부 HTTP 호출은 Feign / WebClient로 일원화하고, 실패 가능 구간은 Spring Retry로 감쌉니다.
- 토큰·시크릿 등 민감 정보는 `AESUtil`로 암호화해 저장합니다.
- 목록 조회 시 무한 스크롤은 `CursorUtil` 기반 커서 페이지네이션을 사용합니다.

<br>

## 👥 Contributors

| <div align="center">[김지민](https://github.com/jinnieusLab)</div>                                    | <div align="center">[김민규](https://github.com/kingmingyu)</div>                                    | <div align="center">[오준영](https://github.com/ojy0903)</div>                                    |
| ----------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| <div align="center"><img src="https://avatars.githubusercontent.com/jinnieusLab" width="160" /></div> | <div align="center"><img src="https://avatars.githubusercontent.com/kingmingyu" width="160" /></div> | <div align="center"><img src="https://avatars.githubusercontent.com/ojy0903" width="160" /></div> |
| <div align="center">백엔드</div>                                                                      | <div align="center">백엔드</div>                                                                     | <div align="center">백엔드</div>                                                                  |


[java]: https://img.shields.io/badge/Java-17-007396?logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMjggMTI4Ij48cGF0aCBmaWxsPSIjRkZGRkZGIiBkPSJNNDcuNjE3IDk4LjEycy00Ljc2NyAyLjc3NCAzLjM5NyAzLjcxYzkuODkyIDEuMTMgMTQuOTQ3Ljk2OCAyNS44NDUtMS4wOTIgMCAwIDIuODcxIDEuNzk1IDYuODczIDMuMzUxLTI0LjQzOSAxMC40Ny01NS4zMDgtLjYwNy0zNi4xMTUtNS45Njl6bS0yLjk4OC0xMy42NjVzLTUuMzQ4IDMuOTU5IDIuODIzIDQuODA1YzEwLjU2NyAxLjA5MSAxOC45MSAxLjE4IDMzLjM1NC0xLjYgMCAwIDEuOTkzIDIuMDI1IDUuMTMyIDMuMTMxLTI5LjU0MiA4LjY0LTYyLjQ0Ni42OC00MS4zMDktNi4zMzZ6Ii8%2BPHBhdGggZmlsbD0iI0ZGRkZGRiIgZD0iTTY5LjgwMiA2MS4yNzFjNi4wMjUgNi45MzUtMS41OCAxMy4xNy0xLjU4IDEzLjE3czE1LjI4OS03Ljg5MSA4LjI2OS0xNy43NzdjLTYuNTU5LTkuMjE1LTExLjU4Ny0xMy43OTIgMTUuNjM1LTI5LjU4IDAgLjAwMS00Mi43MzEgMTAuNjctMjIuMzI0IDM0LjE4N3oiLz48cGF0aCBmaWxsPSIjRkZGRkZGIiBkPSJNMTAyLjEyMyAxMDguMjI5czMuNTI5IDIuOTEtMy44ODggNS4xNTljLTE0LjEwMiA0LjI3Mi01OC43MDYgNS41Ni03MS4wOTQuMTcxLTQuNDUxLTEuOTM4IDMuODk5LTQuNjI1IDYuNTI2LTUuMTkyIDIuNzM5LS41OTMgNC4zMDMtLjQ4NSA0LjMwMy0uNDg1LTQuOTUzLTMuNDg3LTMyLjAxMyA2Ljg1LTEzLjc0MyA5LjgxNSA0OS44MjEgOC4wNzYgOTAuODE3LTMuNjM3IDc3Ljg5Ni05LjQ2OHpNNDkuOTEyIDcwLjI5NHMtMjIuNjg2IDUuMzg5LTguMDMzIDcuMzQ4YzYuMTg4LjgyOCAxOC41MTguNjM4IDMwLjAxMS0uMzI2IDkuMzktLjc4OSAxOC44MTMtMi40NzQgMTguODEzLTIuNDc0cy0zLjMwOCAxLjQxOS01LjcwNCAzLjA1M2MtMjMuMDQyIDYuMDYxLTY3LjU0NCAzLjIzOC01NC43MzEtMi45NTggMTAuODMyLTUuMjM5IDE5LjY0NC00LjY0MyAxOS42NDQtNC42NDN6bTQwLjY5NyAyMi43NDdjMjMuNDIxLTEyLjE2NyAxMi41OTEtMjMuODYgNS4wMzItMjIuMjg1LTEuODQ4LjM4NS0yLjY3Ny43Mi0yLjY3Ny43MnMuNjg4LTEuMDc5IDItMS41NDNjMTQuOTUzLTUuMjU1IDI2LjQ1MSAxNS41MDMtNC44MjMgMjMuNzI1IDAtLjAwMi4zNTktLjMyNy40NjgtLjYxN3oiLz48cGF0aCBmaWxsPSIjRkZGRkZGIiBkPSJNNzYuNDkxIDEuNTg3Uzg5LjQ1OSAxNC41NjMgNjQuMTg4IDM0LjUxYy0yMC4yNjYgMTYuMDA2LTQuNjIxIDI1LjEzLS4wMDcgMzUuNTU5LTExLjgzMS0xMC42NzMtMjAuNTA5LTIwLjA3LTE0LjY4OC0yOC44MTVDNTguMDQxIDI4LjQyIDgxLjcyMiAyMi4xOTUgNzYuNDkxIDEuNTg3eiIvPjxwYXRoIGZpbGw9IiNGRkZGRkYiIGQ9Ik01Mi4yMTQgMTI2LjAyMWMyMi40NzYgMS40MzcgNTctLjggNTcuODE3LTExLjQzNiAwIDAtMS41NzEgNC4wMzItMTguNTc3IDcuMjMxLTE5LjE4NiAzLjYxMi00Mi44NTQgMy4xOTEtNTYuODg3Ljg3NCAwIC4wMDEgMi44NzUgMi4zODEgMTcuNjQ3IDMuMzMxeiIvPjwvc3ZnPg==