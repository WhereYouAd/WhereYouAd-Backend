package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;

public interface AdvertisementCommandService {
    void updateProjectStatus(Long userId, Long orgId, Long projectId, Status status);

    void updateAdContentStatus(Long userId, Long orgId, Long projectId, Long adContentId, Status status);
}
