package com.whereyouad.WhereYouAd.domains.dashboard.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class DashboardException extends AppException {
    public DashboardException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
