package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;

public interface ClickService {

    // 트래킹 링크 생성 메서드
    ClickResponse.NewTrackingUrl createTrackingUrl(Long userId, Long adContentId, Long orgId, String landingUrl);

    // 트래킹 링크 삭제 메서드 (기존 링크 폐기 — 재발급은 삭제 후 발급 API 재호출)
    ClickResponse.DeletedTrackingUrl deleteTrackingUrl(Long userId, Long adContentId, Long orgId);

    // 트래킹 링크 접속 시 랜딩 Url 반환 및 이벤트 생성
    String handleTrackingRedirect(String code, String ipAddress, String userAgent);

    // 실시간 클릭 수 조회 (mode: "real" or "dummy", 최근 N분)
    java.util.List<ClickResponse.RealtimeClickCount> getRealtimeClickCounts(Long adContentId, String mode, int minutes);
}
