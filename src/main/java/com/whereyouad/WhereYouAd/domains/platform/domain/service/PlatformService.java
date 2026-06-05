package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.whereyouad.WhereYouAd.domains.platform.application.dto.request.PlatformRequest;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.PlatformResponse;

public interface PlatformService {
    PlatformResponse.PlatformAccount addNaverAdAccount(Long userId, Long orgId, PlatformRequest.PlatformAccount dto);

    PlatformResponse.PlatformAccountListResponse getPlatformSyncInfos(Long userId, Long orgId);

    PlatformResponse.PlatformAccount updateNaverAdAccount(Long userId, Long orgId, PlatformRequest.PlatformAccount request);

    void disconnectPlatform(Long userId, Long orgId, Long accountId);

    // 회원 탈퇴 스케줄러 등 시스템 내부 호출용 - 요청자 권한 검증 없이 계정 단위 연동 해제
    void disconnectAccountBySystem(Long accountId);
}
