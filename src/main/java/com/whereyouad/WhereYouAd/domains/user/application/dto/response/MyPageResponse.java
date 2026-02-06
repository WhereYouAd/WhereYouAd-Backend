package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

public record MyPageResponse(
        Long userId,
        String email,
        String name,
        String profileImageUrl,
        String phoneNumber,
        boolean isEmailVerified
) {
}
