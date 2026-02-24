package com.whereyouad.WhereYouAd.domains.project.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class ProjectException extends AppException {
    public ProjectException(BaseErrorCode code) {
        super(code);
    }
}
