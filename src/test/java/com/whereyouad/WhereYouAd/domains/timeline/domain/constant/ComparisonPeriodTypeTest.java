package com.whereyouad.WhereYouAd.domains.timeline.domain.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonPeriodTypeTest {

    @Test
    @DisplayName("ComparisonPeriodType은 LAST_WEEK, LAST_MONTH, LAST_YEAR 3개 값을 가짐")
    void hasThreeValues() {
        ComparisonPeriodType[] values = ComparisonPeriodType.values();
        assertThat(values).hasSize(3);
    }

    @Test
    @DisplayName("LAST_WEEK 값이 존재함")
    void lastWeekExists() {
        assertThat(ComparisonPeriodType.valueOf("LAST_WEEK")).isEqualTo(ComparisonPeriodType.LAST_WEEK);
    }

    @Test
    @DisplayName("LAST_MONTH 값이 존재함")
    void lastMonthExists() {
        assertThat(ComparisonPeriodType.valueOf("LAST_MONTH")).isEqualTo(ComparisonPeriodType.LAST_MONTH);
    }

    @Test
    @DisplayName("LAST_YEAR 값이 존재함")
    void lastYearExists() {
        assertThat(ComparisonPeriodType.valueOf("LAST_YEAR")).isEqualTo(ComparisonPeriodType.LAST_YEAR);
    }

    @Test
    @DisplayName("존재하지 않는 값으로 valueOf() 호출 시 IllegalArgumentException 발생")
    void invalidValueThrowsException() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ComparisonPeriodType.valueOf("LAST_DAY"));
    }

    @Test
    @DisplayName("모든 enum 값이 예상된 이름을 가짐")
    void allValuesHaveExpectedNames() {
        assertThat(ComparisonPeriodType.LAST_WEEK.name()).isEqualTo("LAST_WEEK");
        assertThat(ComparisonPeriodType.LAST_MONTH.name()).isEqualTo("LAST_MONTH");
        assertThat(ComparisonPeriodType.LAST_YEAR.name()).isEqualTo("LAST_YEAR");
    }
}