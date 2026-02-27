package com.whereyouad.WhereYouAd.domains.advertisement.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class AdvertisementException extends AppException {
    public AdvertisementException(BaseErrorCode code) {
        super(code);
    }
}
