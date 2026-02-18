package com.whereyouad.WhereYouAd.global.utils.cursor.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class CursorException extends AppException {
    public CursorException(BaseErrorCode code) {
        super(code);
    }
}
