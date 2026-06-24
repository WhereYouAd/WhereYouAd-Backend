package com.whereyouad.WhereYouAd.domains.notification.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class NotificationException extends AppException {
    public NotificationException(BaseErrorCode code) {
        super(code);
    }
}
