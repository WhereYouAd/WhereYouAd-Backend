package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;

public interface OrgService {

    OrgResponse.Create createOrganization(Long userId, OrgRequest.Create request);

    OrgResponse.Read getOrganization(Long userId);

    OrgResponse.Update modifyOrganization(Long userId, Long orgId, OrgRequest.Update request);

    void removeOrganization(Long userId, Long orgId);

    void removeOrganizationSoft(Long userId, Long orgId);

    OrgResponse.Delete restoreOrganization(Long userId, Long orgId);

    // orgId 조직에서 memberId에 해당하는 맴버 제거
    void removeMemberFromOrg(Long userId, Long orgId, Long memberId);
}
