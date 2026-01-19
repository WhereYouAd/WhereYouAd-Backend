package com.whereyouad.WhereYouAd.domains.user.application.mapper;

import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;

public class UserConverter {

    public static SignUpResponse toSignInResponse(User user) {
        return new SignUpResponse(user.getId(), user.getCreatedAt());
    }

}
