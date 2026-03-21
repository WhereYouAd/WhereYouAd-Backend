package com.whereyouad.WhereYouAd.domains.user.application.dto.request;

public record UserInfoModifyRequest(
        String name,
        String oldPassword,
        String newPassword,
        boolean isImageDeleted
) {
}
