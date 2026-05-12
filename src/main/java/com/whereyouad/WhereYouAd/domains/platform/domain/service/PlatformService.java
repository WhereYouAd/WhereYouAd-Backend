package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.whereyouad.WhereYouAd.domains.platform.application.dto.request.PlatformRequest;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.PlatformResponse;

public interface PlatformService {
    PlatformResponse.PlatformAccount addNaverAdAccount(Long userId, Long orgId, PlatformRequest.PlatformAccount dto);

    PlatformResponse.PlatformAccountListResponse getPlatformSyncInfos(Long userId, Long orgId);

    PlatformResponse.PlatformAccount updateNaverAdAccount(Long userId, Long orgId, PlatformRequest.UpdateNaverApiRequest request);
}
