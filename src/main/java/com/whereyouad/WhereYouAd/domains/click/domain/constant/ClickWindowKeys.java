package com.whereyouad.WhereYouAd.domains.click.domain.constant;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 클릭 집계 Redis 키 공용 유틸.
 * ClickConsumer(적재)와 ClickSurgeDetectionService(조회)가 반드시 같은 포맷을 사용해야 한다.
 */
public final class ClickWindowKeys {

    // 클릭 파이프라인 공통 타임존. @Scheduled zone(Asia/Seoul)과 일치해야 하며,
    // 시스템 기본 존에 의존하면 TZ 미설정 서버(UTC)에서 윈도우/일자 경계가 어긋난다
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    public static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private ClickWindowKeys() {
    }

    public static LocalDateTime floorToWindowStart(LocalDateTime time, int windowMinutes) {
        int flooredMinute = (time.getMinute() / windowMinutes) * windowMinutes;
        return time.withMinute(flooredMinute).withSecond(0).withNano(0);
    }

    public static String activeAdsKey(LocalDateTime windowStart) {
        return "click:active:ads:" + windowStart.format(MINUTE_FORMATTER);
    }

    public static String activeAdsMember(Long adContentId, Long orgId) {
        return adContentId + ":" + orgId;
    }

    public static String realMinuteKey(Long adContentId, LocalDateTime minute) {
        return String.format("click:real:%s:%s", adContentId, minute.format(MINUTE_FORMATTER));
    }
}
