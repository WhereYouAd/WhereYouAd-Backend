package com.whereyouad.WhereYouAd.domains.project.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class ProjectHandler extends AppException {
    public ProjectHandler(BaseErrorCode code) {
        super(code);
    }
}
