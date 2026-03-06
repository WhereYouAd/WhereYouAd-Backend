package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;


import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;

public interface AdvertisementQueryService {

    AdvertisementResponse.AdContentInfoResponse readAdContent(Long orgId, Long projectId, Long adContentId);

    AdvertisementResponse.AdContentInfosResponse readAdContents(Long orgId, Long projectId);

    AdvertisementResponse.AdGroupInfoResponse readAdGroup(Long orgId, Long projectId, Long adContentId);
}
