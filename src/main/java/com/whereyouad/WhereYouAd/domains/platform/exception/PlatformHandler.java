package com.whereyouad.WhereYouAd.domains.platform.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class PlatformHandler extends AppException {
    public PlatformHandler(BaseErrorCode code) {
        super(code);
    }
}
