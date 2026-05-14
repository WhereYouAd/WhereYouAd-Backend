package com.whereyouad.WhereYouAd.domains.timeline.application.mapper;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest.TimelineCreateDto;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineConverterTest {

    private Organization buildOrganization(Long id) {
        return Organization.builder()
                .name("테스트 조직")
                .description("조직 설명")
                .ownerUserId(1L)
                .build();
    }

    @Nested
    @DisplayName("toTimeline() - DTO를 Timeline 엔티티로 변환")
    class ToTimeline {

        @Test
        @DisplayName("모든 메트릭이 포함된 경우 모든 use 플래그가 true")
        void allMetricsSelected() {
            Organization org = buildOrganization(1L);
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);
            LocalDate compStart = LocalDate.of(2023, 12, 25);
            LocalDate compEnd = LocalDate.of(2024, 1, 24);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "전체 지표 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK, MetricType.CONVERSION, MetricType.IMPRESSION, MetricType.ROAS),
                    ComparisonPeriodType.LAST_WEEK
            );

            Timeline timeline = TimelineConverter.toTimeline(dto, org, 10L, compStart, compEnd);

            assertThat(timeline.getName()).isEqualTo("전체 지표 타임라인");
            assertThat(timeline.getStartDate()).isEqualTo(start);
            assertThat(timeline.getEndDate()).isEqualTo(end);
            assertThat(timeline.isUseClick()).isTrue();
            assertThat(timeline.isUseConversion()).isTrue();
            assertThat(timeline.isUseImpression()).isTrue();
            assertThat(timeline.isUseRoas()).isTrue();
            assertThat(timeline.getComparisonStartDate()).isEqualTo(compStart);
            assertThat(timeline.getComparisonEndDate()).isEqualTo(compEnd);
            assertThat(timeline.getCreatedBy()).isEqualTo(10L);
            assertThat(timeline.getOrganization()).isEqualTo(org);
        }

        @Test
        @DisplayName("CLICK만 선택한 경우 useClick만 true")
        void onlyClickSelected() {
            Organization org = buildOrganization(1L);
            LocalDate start = LocalDate.of(2024, 2, 1);
            LocalDate end = LocalDate.of(2024, 2, 28);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "클릭 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_MONTH
            );

            Timeline timeline = TimelineConverter.toTimeline(dto, org, 5L,
                    start.minusMonths(1), end.minusMonths(1));

            assertThat(timeline.isUseClick()).isTrue();
            assertThat(timeline.isUseConversion()).isFalse();
            assertThat(timeline.isUseImpression()).isFalse();
            assertThat(timeline.isUseRoas()).isFalse();
        }

        @Test
        @DisplayName("CONVERSION과 ROAS 선택 시 해당 플래그만 true")
        void conversionAndRoasSelected() {
            Organization org = buildOrganization(2L);
            LocalDate start = LocalDate.of(2024, 3, 1);
            LocalDate end = LocalDate.of(2024, 3, 31);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "전환+ROAS 타임라인",
                    start,
                    end,
                    List.of(MetricType.CONVERSION, MetricType.ROAS),
                    ComparisonPeriodType.LAST_YEAR
            );

            Timeline timeline = TimelineConverter.toTimeline(dto, org, 7L,
                    start.minusYears(1), end.minusYears(1));

            assertThat(timeline.isUseClick()).isFalse();
            assertThat(timeline.isUseConversion()).isTrue();
            assertThat(timeline.isUseImpression()).isFalse();
            assertThat(timeline.isUseRoas()).isTrue();
        }

        @Test
        @DisplayName("createdBy가 올바르게 설정됨")
        void createdByIsSetCorrectly() {
            Organization org = buildOrganization(1L);
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);
            Long expectedUserId = 99L;

            TimelineCreateDto dto = new TimelineCreateDto(
                    "타임라인",
                    start,
                    end,
                    List.of(MetricType.IMPRESSION),
                    ComparisonPeriodType.LAST_WEEK
            );

            Timeline timeline = TimelineConverter.toTimeline(dto, org, expectedUserId,
                    start.minusDays(7), end.minusDays(7));

            assertThat(timeline.getCreatedBy()).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("비교 날짜가 올바르게 설정됨")
        void comparisonDatesSetCorrectly() {
            Organization org = buildOrganization(1L);
            LocalDate start = LocalDate.of(2024, 6, 1);
            LocalDate end = LocalDate.of(2024, 6, 30);
            LocalDate compStart = LocalDate.of(2023, 6, 1);
            LocalDate compEnd = LocalDate.of(2023, 6, 30);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "연간 비교 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_YEAR
            );

            Timeline timeline = TimelineConverter.toTimeline(dto, org, 1L, compStart, compEnd);

            assertThat(timeline.getComparisonStartDate()).isEqualTo(compStart);
            assertThat(timeline.getComparisonEndDate()).isEqualTo(compEnd);
        }
    }

    @Nested
    @DisplayName("toCreateResponse() - Timeline 엔티티를 응답 DTO로 변환")
    class ToCreateResponse {

        private Timeline buildTimeline(boolean useClick, boolean useConversion,
                                       boolean useImpression, boolean useRoas) {
            Organization org = Organization.builder()
                    .name("조직")
                    .ownerUserId(1L)
                    .build();

            return Timeline.builder()
                    .name("테스트 타임라인")
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 31))
                    .useClick(useClick)
                    .useConversion(useConversion)
                    .useImpression(useImpression)
                    .useRoas(useRoas)
                    .comparisonStartDate(LocalDate.of(2023, 12, 25))
                    .comparisonEndDate(LocalDate.of(2024, 1, 24))
                    .performanceStatus(PerformanceStatus.ON_TRACK)
                    .createdBy(1L)
                    .organization(org)
                    .build();
        }

        @Test
        @DisplayName("모든 메트릭 플래그가 true일 때 메트릭 목록에 4개 항목 포함")
        void allFlagsTrue_returnsAllMetrics() {
            Timeline timeline = buildTimeline(true, true, true, true);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.metrics()).containsExactlyInAnyOrder(
                    MetricType.CLICK, MetricType.CONVERSION,
                    MetricType.IMPRESSION, MetricType.ROAS
            );
        }

        @Test
        @DisplayName("useClick만 true일 때 메트릭 목록에 CLICK만 포함")
        void onlyClickTrue_returnsOnlyClick() {
            Timeline timeline = buildTimeline(true, false, false, false);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.metrics()).containsExactly(MetricType.CLICK);
        }

        @Test
        @DisplayName("useConversion만 true일 때 메트릭 목록에 CONVERSION만 포함")
        void onlyConversionTrue_returnsOnlyConversion() {
            Timeline timeline = buildTimeline(false, true, false, false);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.metrics()).containsExactly(MetricType.CONVERSION);
        }

        @Test
        @DisplayName("useImpression만 true일 때 메트릭 목록에 IMPRESSION만 포함")
        void onlyImpressionTrue_returnsOnlyImpression() {
            Timeline timeline = buildTimeline(false, false, true, false);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.metrics()).containsExactly(MetricType.IMPRESSION);
        }

        @Test
        @DisplayName("useRoas만 true일 때 메트릭 목록에 ROAS만 포함")
        void onlyRoasTrue_returnsOnlyRoas() {
            Timeline timeline = buildTimeline(false, false, false, true);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.metrics()).containsExactly(MetricType.ROAS);
        }

        @Test
        @DisplayName("모든 플래그가 false일 때 메트릭 목록이 비어 있음")
        void allFlagsFalse_returnsEmptyMetrics() {
            Timeline timeline = buildTimeline(false, false, false, false);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.metrics()).isEmpty();
        }

        @Test
        @DisplayName("응답 DTO에 타임라인 기본 정보가 올바르게 매핑됨")
        void basicFieldsMappedCorrectly() {
            Timeline timeline = buildTimeline(true, false, true, false);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.name()).isEqualTo("테스트 타임라인");
            assertThat(response.startDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(response.endDate()).isEqualTo(LocalDate.of(2024, 1, 31));
            assertThat(response.comparisonStartDate()).isEqualTo(LocalDate.of(2023, 12, 25));
            assertThat(response.comparisonEndDate()).isEqualTo(LocalDate.of(2024, 1, 24));
            assertThat(response.performanceStatus()).isEqualTo(PerformanceStatus.ON_TRACK);
        }

        @Test
        @DisplayName("performanceStatus가 null인 경우도 처리됨")
        void nullPerformanceStatus_handledGracefully() {
            Organization org = Organization.builder()
                    .name("조직")
                    .ownerUserId(1L)
                    .build();

            Timeline timeline = Timeline.builder()
                    .name("타임라인")
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 31))
                    .useClick(true)
                    .useConversion(false)
                    .useImpression(false)
                    .useRoas(false)
                    .comparisonStartDate(LocalDate.of(2023, 12, 25))
                    .comparisonEndDate(LocalDate.of(2024, 1, 24))
                    .performanceStatus(null)
                    .createdBy(1L)
                    .organization(org)
                    .build();

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.performanceStatus()).isNull();
        }

        @Test
        @DisplayName("메트릭 순서: CLICK -> CONVERSION -> IMPRESSION -> ROAS 순서 확인")
        void metricsOrderIsClickConversionImpressionRoas() {
            Timeline timeline = buildTimeline(true, true, true, true);

            TimelineResponse.CreateResponseDTO response = TimelineConverter.toCreateResponse(timeline);

            assertThat(response.metrics()).containsExactly(
                    MetricType.CLICK, MetricType.CONVERSION,
                    MetricType.IMPRESSION, MetricType.ROAS
            );
        }
    }
}