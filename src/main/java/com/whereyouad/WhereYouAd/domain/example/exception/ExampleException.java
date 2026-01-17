package com.whereyouad.WhereYouAd.domain.example.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class ExampleException extends AppException {
    public ExampleException(BaseErrorCode code) {
        super(code);
    }
}
