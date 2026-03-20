package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

public record UserInfoModifiedResponse (
        Long userId,
        String name,
        String profileImageUrl
) {
}
