package com.whereyouad.WhereYouAd.domains.organization.exception.handler;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class OrgHandler extends AppException {
    public OrgHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
