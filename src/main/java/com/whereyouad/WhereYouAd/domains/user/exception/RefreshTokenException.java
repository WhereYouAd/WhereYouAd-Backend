package com.whereyouad.WhereYouAd.domains.user.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class RefreshTokenException extends AppException {
    public RefreshTokenException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
