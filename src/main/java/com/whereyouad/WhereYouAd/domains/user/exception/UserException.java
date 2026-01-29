package com.whereyouad.WhereYouAd.domains.user.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class UserException extends AppException {
    public UserException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
