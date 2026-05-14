package com.whereyouad.WhereYouAd.domains.timeline.domain.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricTypeTest {

    @Test
    @DisplayName("MetricType은 CLICK, CONVERSION, IMPRESSION, ROAS 4개 값을 가짐")
    void hasFourValues() {
        MetricType[] values = MetricType.values();
        assertThat(values).hasSize(4);
    }

    @Test
    @DisplayName("CLICK 값이 존재함")
    void clickExists() {
        assertThat(MetricType.valueOf("CLICK")).isEqualTo(MetricType.CLICK);
    }

    @Test
    @DisplayName("CONVERSION 값이 존재함")
    void conversionExists() {
        assertThat(MetricType.valueOf("CONVERSION")).isEqualTo(MetricType.CONVERSION);
    }

    @Test
    @DisplayName("IMPRESSION 값이 존재함")
    void impressionExists() {
        assertThat(MetricType.valueOf("IMPRESSION")).isEqualTo(MetricType.IMPRESSION);
    }

    @Test
    @DisplayName("ROAS 값이 존재함")
    void roasExists() {
        assertThat(MetricType.valueOf("ROAS")).isEqualTo(MetricType.ROAS);
    }

    @Test
    @DisplayName("존재하지 않는 값으로 valueOf() 호출 시 IllegalArgumentException 발생")
    void invalidValueThrowsException() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> MetricType.valueOf("INVALID_METRIC"));
    }

    @Test
    @DisplayName("모든 enum 값이 예상된 이름을 가짐")
    void allValuesHaveExpectedNames() {
        assertThat(MetricType.CLICK.name()).isEqualTo("CLICK");
        assertThat(MetricType.CONVERSION.name()).isEqualTo("CONVERSION");
        assertThat(MetricType.IMPRESSION.name()).isEqualTo("IMPRESSION");
        assertThat(MetricType.ROAS.name()).isEqualTo("ROAS");
    }
}