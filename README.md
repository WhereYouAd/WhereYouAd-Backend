# WhereYouAd Backend
**2026 Capstone · Graduation Project**

WhereYouAd는 **광고 성과 및 워크스페이스 관리를 효율적으로 지원하는 웹 서비스**입니다.
</br>본 레포지토리는 2026 캡스톤(졸업 프로젝트)을 위해 개발 중인 백엔드 애플리케이션으로,
</br>다중 광고 플랫폼 통합과 안정적인 API 설계, 확장 가능한 도메인 아키텍처를 목표로 합니다.

&nbsp;

## 프로젝트 개요

- **프로젝트명**: WhereYouAd
- **목적**: 광고 및 워크스페이스 관리 서비스의 백엔드 구현
- **성격**: 2026 캡스톤 디자인 · 졸업 프로젝트

### 주요 기능

- **소셜 로그인 & JWT 인증** — Naver / Google / Kakao OAuth2 로그인 및 JWT 세션 관리
- **광고 플랫폼 연동** — Google Ads, Meta Marketing, Naver Ads API와 OAuth2 토큰 기반 연결
- **캠페인 / 광고 관리** — 다중 플랫폼 데이터를 통합 모델로 정규화하여 제공
- **광고 성과 대시보드** — 플랫폼별·기간별 지표 집계 및 커서 기반 페이지네이션
- **클릭 이벤트 수집** — Kafka를 통한 비동기 클릭 이벤트 처리
- **AI 인사이트** — OpenAI 연동을 통한 광고 성과 분석 및 제안
- **워크스페이스 / 프로젝트 관리** — 다중 사용자·팀 단위로 광고 자산을 분리 관리
- **이미지 업로드** — AWS S3를 통한 광고 소재 저장


### 기술 방향성
- 도메인 계층형 아키텍처로 관심사 분리 및 유지보수성 확보
- 외부 API(광고 플랫폼, AI)의 변경에 유연하게 대응할 수 있는 추상화 설계
- 팀 협업을 고려한 컨벤션 및 일관된 응답/예외 처리 구조

&nbsp;

## 팀 구성 (Backend)

| <div align="center">[김지민](https://github.com/jinnieusLab)</div> | <div align="center">[김민규](https://github.com/kingmingyu)</div> | <div align="center">[오준영](https://github.com/ojy0903)</div>                                       |
| --- | --- |---------------------------------------------------------------------------------------------------|
| <div align="center"><img src="https://avatars.githubusercontent.com/jinnieusLab" width="160" /></div> | <div align="center"><img src="https://avatars.githubusercontent.com/kingmingyu" width="160" /></div> | <div align="center"><img src="https://avatars.githubusercontent.com/ojy0903" width="160" /></div> |
| <div align="center">백엔드</div> | <div align="center">백엔드</div> | <div align="center">백엔드</div>                                                                     |

&nbsp;

## 기술 스택

### Core
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.9, Spring Cloud 2024.0.1
- **Build**: Gradle

### Security
- **Auth**: Spring Security, OAuth2 Client
- **Token**: JWT (jjwt 0.11.5)
- **Encryption**: AES 

### DB / Cache
- **RDBMS**: MySQL 8.0
- **ORM**: Spring Data JPA (Hibernate)
- **Cache**: Redis

### Messaging
- **Event Streaming**: Apache Kafka 3.7.0

### External API
- **Ad Platforms**: Google Ads SDK, Meta Marketing API, Naver Ads API
- **AI**: OpenAI API
- **HTTP Client**: OpenFeign, WebClient

### Cloud & Infra
- **Storage**: AWS S3 (Spring Cloud AWS)
- **Notification**: Gmail SMTP, CoolSMS
- **Container**: Docker, Docker Compose

### Docs
- **API 문서**: Springdoc OpenAPI (Swagger UI)

&nbsp;

## 개발 환경

- **JDK**: 17 (LTS 권장)
- **빌드 도구**: Gradle
- **인프라**: Docker / Docker Compose (MySQL 8.0 · Redis · Kafka 로컬 구동)

팀 내 개발 환경 차이를 최소화하기 위해 **JDK 17 기준**으로 개발합니다.
로컬 실행 전에 `.env.example`을 복사하여 `.env` 파일을 생성하고 값을 채워야 합니다.

```bash
cp .env.example .env
```

필요한 환경 변수 그룹:
- DB / Redis / Kafka 접속 정보
- OAuth2 클라이언트 자격증명 (Naver / Google / Kakao)
- 광고 플랫폼 자격증명 (Meta, Google Ads)
- JWT / AES 시크릿
- AWS S3, OpenAI, CoolSMS, Gmail 자격증명

&nbsp;

## 주요 스크립트

| Script | 설명 |
| --- | --- |
| `./gradlew build` | 테스트 포함 전체 빌드 |
| `./gradlew clean build -x test` | 테스트 제외 빌드 |
| `./gradlew test` | 테스트만 실행 (MySQL + Redis 필요) |
| `./gradlew bootRun` | 로컬 서버 실행 (`.env` 필요) |
| `docker-compose up -d` | 전체 스택 실행 (App + MySQL + Redis + Kafka) |

앱이 뜨면 `http://localhost:8080/swagger-ui/index.html` 에서 API 문서를 확인할 수 있습니다.

&nbsp;

## 프로젝트 구조

현재 프로젝트는 **도메인 단위** 및 **계층형 패턴**을 중심으로 구성되어 있습니다.

```bash
src/main/java/com/whereyouad/WhereYouAd/
├── domains/              # 핵심 비즈니스 도메인
│   └── {domain}/
│       ├── application/  # Service 계층 (DTO, Mapper 포함)
│       ├── domain/       # 비즈니스 로직 (Service, Constant)
│       ├── persistence/  # JPA Entity, Repository
│       ├── presentation/ # Controller + Swagger 문서 인터페이스
│       └── exception/    # 도메인 예외 + ErrorCode
├── global/               # 전역 공통 설정 및 유틸
│   ├── security/         # JWT, OAuth2 필터/핸들러
│   ├── config/           # Feign, Redis, Kafka 설정
│   ├── response/         # 응답 래퍼 (BaseResponse, DataResponse)
│   ├── exception/        # 전역 예외 핸들러
│   └── util/             # AESUtil, RedisUtil, CursorUtil 등
└── infrastructure/       # 외부 시스템 연동
    ├── meta/             # Meta Marketing API Feign 클라이언트
    ├── naver/            # Naver Ads API 클라이언트
    ├── openai/           # OpenAI Feign 클라이언트
    ├── s3/               # AWS S3 이미지 업로드
    ├── kafka/            # Kafka Producer / Consumer
    └── notification/     # Gmail SMTP, CoolSMS
```

**핵심 도메인**: `user` · `advertisement` · `project` · `organization` · `platform` · `dashboard` · `image` · `click` · `ai`

&nbsp;

## CI / CD

- **CI** — `.github/workflows/ci.yml`: `main`, `develop` 브랜치에 push / PR 시 MySQL · Redis 서비스 컨테이너를 띄우고 `./gradlew build` 실행
- **CD** — `.github/workflows/cd.yml`: `develop` 브랜치 push 시 Docker 이미지 빌드 → Docker Hub 푸시 → Bastion host 경유 EC2 SSH 배포

&nbsp;

## 협업 컨벤션

### 1. Branch Strategy
- **develop**: 배포 가능한 상태의 브랜치 (Production)
- **feature/#[이슈번호]**: 새로운 기능 개발 브랜치 (예: `feature/#1`)

&nbsp;

### 2. Commit Convention
Angular Commit Convention을 따릅니다.

`type: subject`

| Type | 설명 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (README 등) |
| `style` | 코드 포맷팅, 세미콜론 누락, 공백 등 (코드 변경 없음) |
| `refactor` | 코드 리팩토링 (기능 변화 없음) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드, 설정, 패키지 매니저 등 기타 변경사항 |
| `ci` | CI 관련 설정 변경 |
| `setting` | 프로젝트 환경 설정 변경 |

&nbsp;

### 3. Code Style

팀 전체의 코드 스타일 일관성을 위해 다음 규칙을 적용합니다.

- **패키지 구조**: 도메인 단위 계층형 패키지 (`application` / `domain` / `persistence` / `presentation` / `exception`)
- **응답 형식**: 모든 API 응답은 `BaseResponse` 계열 래퍼(`DataResponse<T>`, `ErrorResponse`)로 통일
- **에러 처리**: 도메인별 `BaseErrorCode` 구현체로 에러 코드 정의 → 도메인 예외 throw → 전역 핸들러 매핑
- **API 문서**: Controller 코드와 Swagger 어노테이션을 `presentation/docs/` 인터페이스로 분리

&nbsp;

### 4. PR Process
PR 템플릿에 따라 작성하며, **리뷰어 가이드**를 준수합니다.

- **Title**: `[Type/#이슈번호] 작업 요약` (예: `[Feat/#1] 소셜 로그인 API 구현`)
- **Review Labels**:
  - `P1`: **필수 반영 (Critical)** - 버그/컨벤션 위반 (머지 불가)
  - `P2`: **적극 권장 (Recommended)** - 더 나은 대안 (반영 권장)
  - `P3`: **제안 (Suggestion)** - 아이디어 공유 (자율)
  - `P4`: **단순 확인 (Nit)** - 오타/칭찬 등
