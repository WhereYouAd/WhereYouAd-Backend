package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;

public interface OrgQueryService {

    // 조직에 속한 맴버 조회 (무한 스크롤 - Slice 기반)
    OrgResponse.OrgMemberSliceDTO getOrgMembers(Long orgId, String cursor, Integer size);

    // 조직의 전체 멤버 수 조회
    OrgResponse.OrgMemberCountDTO getOrgMembersCount(Long orgId);
}
