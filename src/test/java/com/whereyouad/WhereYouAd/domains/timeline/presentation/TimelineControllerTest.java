package com.whereyouad.WhereYouAd.domains.timeline.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest.TimelineCreateDto;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.domain.service.TimelineService;
import com.whereyouad.WhereYouAd.domains.timeline.exception.TimelineException;
import com.whereyouad.WhereYouAd.domains.timeline.exception.code.TimelineErrorCode;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TimelineControllerTest {

    @Mock
    private TimelineService timelineService;

    @InjectMocks
    private TimelineController timelineController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Minimal principal stub for @AuthenticationPrincipal(expression = "userId")
    static class UserIdPrincipal {
        private final Long userId;

        UserIdPrincipal(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(Long userId) {
        UserIdPrincipal principal = new UserIdPrincipal(userId);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(timelineController)
                .build();
    }

    @Nested
    @DisplayName("POST /api/org/{orgId}/timeline - 타임라인 생성")
    class CreateTimeline {

        @Test
        @DisplayName("유효한 요청으로 타임라인 생성 시 201 반환")
        void createTimeline_validRequest_returns201() throws Exception {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "테스트 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK, MetricType.IMPRESSION),
                    ComparisonPeriodType.LAST_WEEK
            );

            TimelineResponse.CreateResponseDTO responseDto = new TimelineResponse.CreateResponseDTO(
                    1L,
                    "테스트 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK, MetricType.IMPRESSION),
                    start.minusDays(7),
                    end.minusDays(7),
                    PerformanceStatus.ON_TRACK,
                    LocalDateTime.of(2024, 1, 1, 0, 0)
            );

            given(timelineService.createTimeline(eq(userId), eq(orgId), any(TimelineCreateDto.class)))
                    .willReturn(responseDto);

            mockMvc.perform(post("/api/org/{orgId}/timeline", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isCreated());

            verify(timelineService, times(1)).createTimeline(eq(userId), eq(orgId), any(TimelineCreateDto.class));
        }

        @Test
        @DisplayName("이름이 빈 문자열인 경우 400 반환")
        void createTimeline_blankName_returns400() throws Exception {
            Long orgId = 10L;
            Long userId = 1L;

            String requestBody = """
                    {
                        "name": "",
                        "startDate": "2024-01-01",
                        "endDate": "2024-01-31",
                        "metrics": ["CLICK"],
                        "comparisonPeriodType": "LAST_WEEK"
                    }
                    """;

            mockMvc.perform(post("/api/org/{orgId}/timeline", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("메트릭 목록이 빈 경우 400 반환")
        void createTimeline_emptyMetrics_returns400() throws Exception {
            Long orgId = 10L;
            Long userId = 1L;

            String requestBody = """
                    {
                        "name": "타임라인",
                        "startDate": "2024-01-01",
                        "endDate": "2024-01-31",
                        "metrics": [],
                        "comparisonPeriodType": "LAST_WEEK"
                    }
                    """;

            mockMvc.perform(post("/api/org/{orgId}/timeline", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("startDate가 null인 경우 400 반환")
        void createTimeline_nullStartDate_returns400() throws Exception {
            Long orgId = 10L;
            Long userId = 1L;

            String requestBody = """
                    {
                        "name": "타임라인",
                        "startDate": null,
                        "endDate": "2024-01-31",
                        "metrics": ["CLICK"],
                        "comparisonPeriodType": "LAST_WEEK"
                    }
                    """;

            mockMvc.perform(post("/api/org/{orgId}/timeline", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("endDate가 null인 경우 400 반환")
        void createTimeline_nullEndDate_returns400() throws Exception {
            Long orgId = 10L;
            Long userId = 1L;

            String requestBody = """
                    {
                        "name": "타임라인",
                        "startDate": "2024-01-01",
                        "endDate": null,
                        "metrics": ["CLICK"],
                        "comparisonPeriodType": "LAST_WEEK"
                    }
                    """;

            mockMvc.perform(post("/api/org/{orgId}/timeline", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("comparisonPeriodType이 null인 경우 400 반환")
        void createTimeline_nullComparisonPeriodType_returns400() throws Exception {
            Long orgId = 10L;
            Long userId = 1L;

            String requestBody = """
                    {
                        "name": "타임라인",
                        "startDate": "2024-01-01",
                        "endDate": "2024-01-31",
                        "metrics": ["CLICK"],
                        "comparisonPeriodType": null
                    }
                    """;

            mockMvc.perform(post("/api/org/{orgId}/timeline", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("응답 본문에 생성된 타임라인 데이터가 포함됨")
        void createTimeline_responseBodyContainsCreatedData() throws Exception {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "응답 확인 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_MONTH
            );

            TimelineResponse.CreateResponseDTO responseDto = new TimelineResponse.CreateResponseDTO(
                    42L,
                    "응답 확인 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK),
                    start.minusMonths(1),
                    end.minusMonths(1),
                    PerformanceStatus.ABOVE_AVG,
                    LocalDateTime.of(2024, 1, 1, 12, 0)
            );

            given(timelineService.createTimeline(eq(userId), eq(orgId), any(TimelineCreateDto.class)))
                    .willReturn(responseDto);

            mockMvc.perform(post("/api/org/{orgId}/timeline", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.timelineId").value(42))
                    .andExpect(jsonPath("$.data.name").value("응답 확인 타임라인"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/org/{orgId}/timeline/{timelineId} - 타임라인 삭제")
    class DeleteTimeline {

        @Test
        @DisplayName("유효한 요청으로 타임라인 삭제 시 200 반환")
        void deleteTimeline_validRequest_returns200() throws Exception {
            Long userId = 1L;
            Long orgId = 10L;
            Long timelineId = 100L;

            willDoNothing().given(timelineService).deleteTimeline(userId, orgId, timelineId);

            mockMvc.perform(delete("/api/org/{orgId}/timeline/{timelineId}", orgId, timelineId)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isOk());

            verify(timelineService, times(1)).deleteTimeline(userId, orgId, timelineId);
        }

        @Test
        @DisplayName("타임라인 삭제 시 서비스 메서드가 올바른 인자로 호출됨")
        void deleteTimeline_correctArgumentsPassed() throws Exception {
            Long userId = 5L;
            Long orgId = 20L;
            Long timelineId = 300L;

            willDoNothing().given(timelineService).deleteTimeline(userId, orgId, timelineId);

            mockMvc.perform(delete("/api/org/{orgId}/timeline/{timelineId}", orgId, timelineId)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isOk());

            verify(timelineService).deleteTimeline(userId, orgId, timelineId);
        }

        @Test
        @DisplayName("타임라인 삭제 성공 시 응답 상태가 OK")
        void deleteTimeline_success_returnsOkStatus() throws Exception {
            Long userId = 1L;
            Long orgId = 10L;
            Long timelineId = 100L;

            willDoNothing().given(timelineService).deleteTimeline(userId, orgId, timelineId);

            mockMvc.perform(delete("/api/org/{orgId}/timeline/{timelineId}", orgId, timelineId)
                            .with(authentication(buildAuthentication(userId))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
