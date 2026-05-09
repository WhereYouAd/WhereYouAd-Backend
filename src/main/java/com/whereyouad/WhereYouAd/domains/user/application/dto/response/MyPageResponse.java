package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

import java.util.List;

public record MyPageResponse(
        Long userId,
        String email,
        String name,
        String profileImageUrl,
        String phoneNumber,
        boolean isEmailVerified,
        String providerType,
        List<MyOrgResponse> organizations
) {
}
