package com.whereyouad.WhereYouAd.domains.ai.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class AIHandler extends AppException {
    public AIHandler(BaseErrorCode code) {
        super(code);
    }
}