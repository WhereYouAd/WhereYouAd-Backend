package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;

public interface OrgService {

    OrgResponse.Create createOrganization(Long userId, OrgRequest.Create request);

    OrgResponse.Read getOrganization(Long userId);

    OrgResponse.Update modifyOrganization(Long userId, Long orgId, OrgRequest.Update request);

    void removeOrganization(Long userId, Long orgId);
}
