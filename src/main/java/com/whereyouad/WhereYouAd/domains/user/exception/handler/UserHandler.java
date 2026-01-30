package com.whereyouad.WhereYouAd.domains.user.exception.handler;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class UserHandler extends AppException {
    public UserHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
