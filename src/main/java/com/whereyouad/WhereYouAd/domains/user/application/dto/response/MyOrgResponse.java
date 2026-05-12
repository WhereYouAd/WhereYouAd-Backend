package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;

public record MyOrgResponse(
        Long orgId,
        String orgName,
        OrgRole myRole
) {
}
