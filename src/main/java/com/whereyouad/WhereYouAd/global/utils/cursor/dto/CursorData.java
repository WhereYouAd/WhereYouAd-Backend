package com.whereyouad.WhereYouAd.global.utils.cursor.dto;

import com.whereyouad.WhereYouAd.global.utils.cursor.exception.CursorException;
import com.whereyouad.WhereYouAd.global.utils.cursor.exception.code.CursorErrorCode;

public record CursorData(
        Long id) {
    public CursorData {
        if (id == null || id <= 0) {
            throw new CursorException(CursorErrorCode.ID_NOT_POSITIVE_NUMBER);
        }
    }
}
