package com.whereyouad.WhereYouAd.domains.timeline.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class TimelineException extends AppException {
    public TimelineException(BaseErrorCode code) {
        super(code);
    }
}
