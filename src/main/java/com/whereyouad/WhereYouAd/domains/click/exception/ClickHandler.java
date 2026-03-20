package com.whereyouad.WhereYouAd.domains.click.exception;

import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.BaseErrorCode;

public class ClickHandler extends AppException {
    public ClickHandler(BaseErrorCode code) { super(code); }
}
