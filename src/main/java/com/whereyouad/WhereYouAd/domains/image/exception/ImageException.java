package com.whereyouad.WhereYouAd.domains.image.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class ImageException extends AppException{
    public ImageException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
