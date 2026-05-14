package com.whereyouad.WhereYouAd.domains.timeline.exception.code;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineErrorCodeTest {

    @Test
    @DisplayName("TIMELINE_INVALID_DATE_RANGE는 400 상태 코드를 가짐")
    void invalidDateRange_hasBadRequestStatus() {
        assertThat(TimelineErrorCode.TIMELINE_INVALID_DATE_RANGE.getHttpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("TIMELINE_INVALID_DATE_RANGE는 올바른 코드 문자열을 가짐")
    void invalidDateRange_hasCorrectCode() {
        assertThat(TimelineErrorCode.TIMELINE_INVALID_DATE_RANGE.getCode())
                .isEqualTo("TIMELINE_400_1");
    }

    @Test
    @DisplayName("TIMELINE_INVALID_DATE_RANGE는 비어 있지 않은 메시지를 가짐")
    void invalidDateRange_hasNonEmptyMessage() {
        assertThat(TimelineErrorCode.TIMELINE_INVALID_DATE_RANGE.getMessage())
                .isNotBlank();
    }

    @Test
    @DisplayName("TIMELINE_FORBIDDEN은 403 상태 코드를 가짐")
    void forbidden_hasForbiddenStatus() {
        assertThat(TimelineErrorCode.TIMELINE_FORBIDDEN.getHttpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("TIMELINE_FORBIDDEN은 올바른 코드 문자열을 가짐")
    void forbidden_hasCorrectCode() {
        assertThat(TimelineErrorCode.TIMELINE_FORBIDDEN.getCode())
                .isEqualTo("TIMELINE_403_1");
    }

    @Test
    @DisplayName("TIMELINE_FORBIDDEN는 비어 있지 않은 메시지를 가짐")
    void forbidden_hasNonEmptyMessage() {
        assertThat(TimelineErrorCode.TIMELINE_FORBIDDEN.getMessage())
                .isNotBlank();
    }

    @Test
    @DisplayName("TIMELINE_NOT_FOUND는 404 상태 코드를 가짐")
    void notFound_hasNotFoundStatus() {
        assertThat(TimelineErrorCode.TIMELINE_NOT_FOUND.getHttpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("TIMELINE_NOT_FOUND는 올바른 코드 문자열을 가짐")
    void notFound_hasCorrectCode() {
        assertThat(TimelineErrorCode.TIMELINE_NOT_FOUND.getCode())
                .isEqualTo("TIMELINE_404_1");
    }

    @Test
    @DisplayName("TIMELINE_NOT_FOUND는 비어 있지 않은 메시지를 가짐")
    void notFound_hasNonEmptyMessage() {
        assertThat(TimelineErrorCode.TIMELINE_NOT_FOUND.getMessage())
                .isNotBlank();
    }

    @Test
    @DisplayName("모든 에러 코드가 3개 존재함")
    void hasThreeErrorCodes() {
        assertThat(TimelineErrorCode.values()).hasSize(3);
    }

    @Test
    @DisplayName("각 에러 코드는 고유한 code 값을 가짐")
    void eachErrorCodeHasUniqueCode() {
        TimelineErrorCode[] codes = TimelineErrorCode.values();
        long distinctCount = java.util.Arrays.stream(codes)
                .map(TimelineErrorCode::getCode)
                .distinct()
                .count();
        assertThat(distinctCount).isEqualTo(codes.length);
    }
}