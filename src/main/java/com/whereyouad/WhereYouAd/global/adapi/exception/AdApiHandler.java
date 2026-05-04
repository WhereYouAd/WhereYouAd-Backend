package com.whereyouad.WhereYouAd.global.adapi.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class AdApiHandler extends AppException {
    public AdApiHandler(BaseErrorCode code) {
        super(code);
    }
}
