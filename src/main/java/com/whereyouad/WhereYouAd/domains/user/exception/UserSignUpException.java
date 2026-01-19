package com.whereyouad.WhereYouAd.domains.user.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class UserSignUpException extends AppException {
    public UserSignUpException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
