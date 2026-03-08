package com.whereyouad.WhereYouAd.domains.advertisement.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class AdvertisementHandler extends AppException {
    public AdvertisementHandler(BaseErrorCode code) {
        super(code);
    }
}
