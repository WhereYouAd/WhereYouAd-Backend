package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.request.TimelineRequest.TimelineCreateDto;
import com.whereyouad.WhereYouAd.domains.timeline.application.dto.response.TimelineResponse;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.ComparisonPeriodType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.MetricType;
import com.whereyouad.WhereYouAd.domains.timeline.domain.constant.PerformanceStatus;
import com.whereyouad.WhereYouAd.domains.timeline.domain.util.TimelineUtil;
import com.whereyouad.WhereYouAd.domains.timeline.exception.TimelineException;
import com.whereyouad.WhereYouAd.domains.timeline.exception.code.TimelineErrorCode;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.repository.TimelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceImplTest {

    @Mock
    private TimelineRepository timelineRepository;

    @Mock
    private OrgRepository orgRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private TimelineUtil timelineUtil;

    @InjectMocks
    private TimelineServiceImpl timelineService;

    private Organization organization;
    private OrgMember adminMember;
    private OrgMember normalMember;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .name("테스트 조직")
                .description("설명")
                .ownerUserId(1L)
                .build();

        adminMember = OrgMember.builder()
                .role(OrgRole.ADMIN)
                .build();

        normalMember = OrgMember.builder()
                .role(OrgRole.MEMBER)
                .build();
    }

    @Nested
    @DisplayName("createTimeline() - 타임라인 생성")
    class CreateTimeline {

        @Test
        @DisplayName("정상적인 요청으로 타임라인 생성 성공")
        void createTimeline_success() {
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

            Timeline savedTimeline = Timeline.builder()
                    .name("테스트 타임라인")
                    .startDate(start)
                    .endDate(end)
                    .useClick(true)
                    .useConversion(false)
                    .useImpression(true)
                    .useRoas(false)
                    .comparisonStartDate(start.minusDays(7))
                    .comparisonEndDate(end.minusDays(7))
                    .performanceStatus(PerformanceStatus.ON_TRACK)
                    .createdBy(userId)
                    .organization(organization)
                    .build();

            given(orgRepository.findById(orgId)).willReturn(Optional.of(organization));
            given(timelineUtil.calculatePerformanceStatus(any(Timeline.class)))
                    .willReturn(PerformanceStatus.ON_TRACK);
            given(timelineRepository.save(any(Timeline.class))).willReturn(savedTimeline);

            TimelineResponse.CreateResponseDTO result = timelineService.createTimeline(userId, orgId, dto);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("테스트 타임라인");
            assertThat(result.startDate()).isEqualTo(start);
            assertThat(result.endDate()).isEqualTo(end);
            verify(timelineRepository, times(1)).save(any(Timeline.class));
        }

        @Test
        @DisplayName("존재하지 않는 조직 ID로 생성 시 OrgHandler 예외 발생")
        void createTimeline_orgNotFound_throwsOrgHandler() {
            Long userId = 1L;
            Long orgId = 999L;

            TimelineCreateDto dto = new TimelineCreateDto(
                    "타임라인",
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 31),
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_WEEK
            );

            given(orgRepository.findById(orgId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> timelineService.createTimeline(userId, orgId, dto))
                    .isInstanceOf(OrgHandler.class)
                    .satisfies(ex -> assertThat(((OrgHandler) ex).getErrorCode())
                            .isEqualTo(OrgErrorCode.ORG_NOT_FOUND));
        }

        @Test
        @DisplayName("종료일이 시작일보다 앞선 경우 TIMELINE_INVALID_DATE_RANGE 예외 발생")
        void createTimeline_endBeforeStart_throwsTimelineException() {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate start = LocalDate.of(2024, 1, 31);
            LocalDate end = LocalDate.of(2024, 1, 1); // end < start

            TimelineCreateDto dto = new TimelineCreateDto(
                    "잘못된 날짜 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_WEEK
            );

            given(orgRepository.findById(orgId)).willReturn(Optional.of(organization));

            assertThatThrownBy(() -> timelineService.createTimeline(userId, orgId, dto))
                    .isInstanceOf(TimelineException.class)
                    .satisfies(ex -> assertThat(((TimelineException) ex).getErrorCode())
                            .isEqualTo(TimelineErrorCode.TIMELINE_INVALID_DATE_RANGE));
        }

        @Test
        @DisplayName("시작일과 종료일이 같은 경우 타임라인 생성 성공")
        void createTimeline_sameDates_success() {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate same = LocalDate.of(2024, 1, 15);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "하루 타임라인",
                    same,
                    same,
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_WEEK
            );

            Timeline savedTimeline = Timeline.builder()
                    .name("하루 타임라인")
                    .startDate(same)
                    .endDate(same)
                    .useClick(true)
                    .useConversion(false)
                    .useImpression(false)
                    .useRoas(false)
                    .comparisonStartDate(same.minusDays(7))
                    .comparisonEndDate(same.minusDays(7))
                    .performanceStatus(null)
                    .createdBy(userId)
                    .organization(organization)
                    .build();

            given(orgRepository.findById(orgId)).willReturn(Optional.of(organization));
            given(timelineUtil.calculatePerformanceStatus(any(Timeline.class))).willReturn(null);
            given(timelineRepository.save(any(Timeline.class))).willReturn(savedTimeline);

            TimelineResponse.CreateResponseDTO result = timelineService.createTimeline(userId, orgId, dto);

            assertThat(result).isNotNull();
            verify(timelineRepository, times(1)).save(any(Timeline.class));
        }

        @Test
        @DisplayName("LAST_WEEK 비교 기간: 비교 날짜가 7일 전으로 계산됨")
        void createTimeline_lastWeekComparison_calculatesDatesCorrectly() {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate start = LocalDate.of(2024, 2, 7);
            LocalDate end = LocalDate.of(2024, 2, 14);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "지난 주 비교",
                    start,
                    end,
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_WEEK
            );

            ArgumentCaptor<Timeline> captor = ArgumentCaptor.forClass(Timeline.class);

            Timeline savedTimeline = Timeline.builder()
                    .name("지난 주 비교")
                    .startDate(start)
                    .endDate(end)
                    .useClick(true)
                    .useConversion(false)
                    .useImpression(false)
                    .useRoas(false)
                    .comparisonStartDate(start.minusDays(7))
                    .comparisonEndDate(end.minusDays(7))
                    .performanceStatus(null)
                    .createdBy(userId)
                    .organization(organization)
                    .build();

            given(orgRepository.findById(orgId)).willReturn(Optional.of(organization));
            given(timelineUtil.calculatePerformanceStatus(any())).willReturn(null);
            given(timelineRepository.save(any(Timeline.class))).willReturn(savedTimeline);

            timelineService.createTimeline(userId, orgId, dto);

            verify(timelineRepository).save(captor.capture());
            Timeline captured = captor.getValue();
            assertThat(captured.getComparisonStartDate()).isEqualTo(start.minusDays(7));
            assertThat(captured.getComparisonEndDate()).isEqualTo(end.minusDays(7));
        }

        @Test
        @DisplayName("LAST_MONTH 비교 기간: 비교 날짜가 1달 전으로 계산됨")
        void createTimeline_lastMonthComparison_calculatesDatesCorrectly() {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate start = LocalDate.of(2024, 3, 1);
            LocalDate end = LocalDate.of(2024, 3, 31);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "지난 달 비교",
                    start,
                    end,
                    List.of(MetricType.CONVERSION),
                    ComparisonPeriodType.LAST_MONTH
            );

            ArgumentCaptor<Timeline> captor = ArgumentCaptor.forClass(Timeline.class);

            Timeline savedTimeline = Timeline.builder()
                    .name("지난 달 비교")
                    .startDate(start)
                    .endDate(end)
                    .useClick(false)
                    .useConversion(true)
                    .useImpression(false)
                    .useRoas(false)
                    .comparisonStartDate(start.minusMonths(1))
                    .comparisonEndDate(end.minusMonths(1))
                    .performanceStatus(null)
                    .createdBy(userId)
                    .organization(organization)
                    .build();

            given(orgRepository.findById(orgId)).willReturn(Optional.of(organization));
            given(timelineUtil.calculatePerformanceStatus(any())).willReturn(null);
            given(timelineRepository.save(any(Timeline.class))).willReturn(savedTimeline);

            timelineService.createTimeline(userId, orgId, dto);

            verify(timelineRepository).save(captor.capture());
            Timeline captured = captor.getValue();
            assertThat(captured.getComparisonStartDate()).isEqualTo(start.minusMonths(1));
            assertThat(captured.getComparisonEndDate()).isEqualTo(end.minusMonths(1));
        }

        @Test
        @DisplayName("LAST_YEAR 비교 기간: 비교 날짜가 1년 전으로 계산됨")
        void createTimeline_lastYearComparison_calculatesDatesCorrectly() {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate start = LocalDate.of(2024, 6, 1);
            LocalDate end = LocalDate.of(2024, 6, 30);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "지난 해 비교",
                    start,
                    end,
                    List.of(MetricType.ROAS),
                    ComparisonPeriodType.LAST_YEAR
            );

            ArgumentCaptor<Timeline> captor = ArgumentCaptor.forClass(Timeline.class);

            Timeline savedTimeline = Timeline.builder()
                    .name("지난 해 비교")
                    .startDate(start)
                    .endDate(end)
                    .useClick(false)
                    .useConversion(false)
                    .useImpression(false)
                    .useRoas(true)
                    .comparisonStartDate(start.minusYears(1))
                    .comparisonEndDate(end.minusYears(1))
                    .performanceStatus(null)
                    .createdBy(userId)
                    .organization(organization)
                    .build();

            given(orgRepository.findById(orgId)).willReturn(Optional.of(organization));
            given(timelineUtil.calculatePerformanceStatus(any())).willReturn(null);
            given(timelineRepository.save(any(Timeline.class))).willReturn(savedTimeline);

            timelineService.createTimeline(userId, orgId, dto);

            verify(timelineRepository).save(captor.capture());
            Timeline captured = captor.getValue();
            assertThat(captured.getComparisonStartDate()).isEqualTo(start.minusYears(1));
            assertThat(captured.getComparisonEndDate()).isEqualTo(end.minusYears(1));
        }

        @Test
        @DisplayName("performanceStatus가 Timeline에 설정되어 저장됨")
        void createTimeline_performanceStatusIsSetBeforeSave() {
            Long userId = 1L;
            Long orgId = 10L;
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);

            TimelineCreateDto dto = new TimelineCreateDto(
                    "성과 타임라인",
                    start,
                    end,
                    List.of(MetricType.CLICK),
                    ComparisonPeriodType.LAST_WEEK
            );

            ArgumentCaptor<Timeline> captor = ArgumentCaptor.forClass(Timeline.class);

            Timeline savedTimeline = Timeline.builder()
                    .name("성과 타임라인")
                    .startDate(start)
                    .endDate(end)
                    .useClick(true)
                    .useConversion(false)
                    .useImpression(false)
                    .useRoas(false)
                    .comparisonStartDate(start.minusDays(7))
                    .comparisonEndDate(end.minusDays(7))
                    .performanceStatus(PerformanceStatus.ABOVE_AVG)
                    .createdBy(userId)
                    .organization(organization)
                    .build();

            given(orgRepository.findById(orgId)).willReturn(Optional.of(organization));
            given(timelineUtil.calculatePerformanceStatus(any(Timeline.class)))
                    .willReturn(PerformanceStatus.ABOVE_AVG);
            given(timelineRepository.save(any(Timeline.class))).willReturn(savedTimeline);

            timelineService.createTimeline(userId, orgId, dto);

            verify(timelineRepository).save(captor.capture());
            assertThat(captor.getValue().getPerformanceStatus()).isEqualTo(PerformanceStatus.ABOVE_AVG);
        }
    }

    @Nested
    @DisplayName("deleteTimeline() - 타임라인 삭제")
    class DeleteTimeline {

        private Timeline buildTimelineWithOrg(Organization org) {
            return Timeline.builder()
                    .name("삭제할 타임라인")
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 1, 31))
                    .useClick(true)
                    .useConversion(false)
                    .useImpression(false)
                    .useRoas(false)
                    .comparisonStartDate(LocalDate.of(2023, 12, 25))
                    .comparisonEndDate(LocalDate.of(2024, 1, 24))
                    .performanceStatus(PerformanceStatus.ON_TRACK)
                    .createdBy(1L)
                    .organization(org)
                    .build();
        }

        @Test
        @DisplayName("ADMIN 권한 멤버가 삭제 요청 시 정상 삭제됨")
        void deleteTimeline_adminMember_success() {
            Long userId = 1L;
            Long orgId = 10L;
            Long timelineId = 100L;

            Organization orgWithId = Organization.builder()
                    .name("테스트 조직")
                    .ownerUserId(1L)
                    .build();

            // We need the org to have id=10L but since builder doesn't set id for new entity,
            // we use reflection or mock. Here we use a spy approach instead.
            // The test validates orgId matching via timeline.getOrganization().getId()
            // Since we can't set id via builder for Organization (auto-generated),
            // we use mockito to mock the timeline's organization getId() call.
            Timeline mockTimeline = mock(Timeline.class);
            Organization mockOrg = mock(Organization.class);

            given(timelineRepository.findById(timelineId)).willReturn(Optional.of(mockTimeline));
            given(mockTimeline.getOrganization()).willReturn(mockOrg);
            given(mockOrg.getId()).willReturn(orgId);
            given(orgMemberRepository.findByUserIdAndOrgId(userId, orgId))
                    .willReturn(Optional.of(adminMember));

            timelineService.deleteTimeline(userId, orgId, timelineId);

            verify(timelineRepository, times(1)).delete(mockTimeline);
        }

        @Test
        @DisplayName("존재하지 않는 타임라인 삭제 시 TIMELINE_NOT_FOUND 예외 발생")
        void deleteTimeline_timelineNotFound_throwsException() {
            Long userId = 1L;
            Long orgId = 10L;
            Long timelineId = 999L;

            given(timelineRepository.findById(timelineId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> timelineService.deleteTimeline(userId, orgId, timelineId))
                    .isInstanceOf(TimelineException.class)
                    .satisfies(ex -> assertThat(((TimelineException) ex).getErrorCode())
                            .isEqualTo(TimelineErrorCode.TIMELINE_NOT_FOUND));

            verify(timelineRepository, never()).delete(any());
        }

        @Test
        @DisplayName("타임라인의 조직 ID가 요청 조직 ID와 다를 경우 OrgHandler 예외 발생")
        void deleteTimeline_orgMismatch_throwsOrgHandler() {
            Long userId = 1L;
            Long orgId = 10L;
            Long differentOrgId = 20L;
            Long timelineId = 100L;

            Timeline mockTimeline = mock(Timeline.class);
            Organization mockOrg = mock(Organization.class);

            given(timelineRepository.findById(timelineId)).willReturn(Optional.of(mockTimeline));
            given(mockTimeline.getOrganization()).willReturn(mockOrg);
            given(mockOrg.getId()).willReturn(differentOrgId); // 다른 조직 ID

            assertThatThrownBy(() -> timelineService.deleteTimeline(userId, orgId, timelineId))
                    .isInstanceOf(OrgHandler.class)
                    .satisfies(ex -> assertThat(((OrgHandler) ex).getErrorCode())
                            .isEqualTo(OrgErrorCode.ORG_NOT_FOUND));

            verify(timelineRepository, never()).delete(any());
        }

        @Test
        @DisplayName("조직 멤버가 아닌 사용자가 삭제 요청 시 TIMELINE_FORBIDDEN 예외 발생")
        void deleteTimeline_notOrgMember_throwsForbiddenException() {
            Long userId = 99L;
            Long orgId = 10L;
            Long timelineId = 100L;

            Timeline mockTimeline = mock(Timeline.class);
            Organization mockOrg = mock(Organization.class);

            given(timelineRepository.findById(timelineId)).willReturn(Optional.of(mockTimeline));
            given(mockTimeline.getOrganization()).willReturn(mockOrg);
            given(mockOrg.getId()).willReturn(orgId);
            given(orgMemberRepository.findByUserIdAndOrgId(userId, orgId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> timelineService.deleteTimeline(userId, orgId, timelineId))
                    .isInstanceOf(TimelineException.class)
                    .satisfies(ex -> assertThat(((TimelineException) ex).getErrorCode())
                            .isEqualTo(TimelineErrorCode.TIMELINE_FORBIDDEN));

            verify(timelineRepository, never()).delete(any());
        }

        @Test
        @DisplayName("MEMBER 역할인 사용자가 삭제 요청 시 TIMELINE_FORBIDDEN 예외 발생")
        void deleteTimeline_memberRole_throwsForbiddenException() {
            Long userId = 2L;
            Long orgId = 10L;
            Long timelineId = 100L;

            Timeline mockTimeline = mock(Timeline.class);
            Organization mockOrg = mock(Organization.class);

            given(timelineRepository.findById(timelineId)).willReturn(Optional.of(mockTimeline));
            given(mockTimeline.getOrganization()).willReturn(mockOrg);
            given(mockOrg.getId()).willReturn(orgId);
            given(orgMemberRepository.findByUserIdAndOrgId(userId, orgId))
                    .willReturn(Optional.of(normalMember));

            assertThatThrownBy(() -> timelineService.deleteTimeline(userId, orgId, timelineId))
                    .isInstanceOf(TimelineException.class)
                    .satisfies(ex -> assertThat(((TimelineException) ex).getErrorCode())
                            .isEqualTo(TimelineErrorCode.TIMELINE_FORBIDDEN));

            verify(timelineRepository, never()).delete(any());
        }

        @Test
        @DisplayName("삭제 성공 시 timelineRepository.delete()가 정확히 1번 호출됨")
        void deleteTimeline_success_deleteCalled() {
            Long userId = 1L;
            Long orgId = 10L;
            Long timelineId = 100L;

            Timeline mockTimeline = mock(Timeline.class);
            Organization mockOrg = mock(Organization.class);

            given(timelineRepository.findById(timelineId)).willReturn(Optional.of(mockTimeline));
            given(mockTimeline.getOrganization()).willReturn(mockOrg);
            given(mockOrg.getId()).willReturn(orgId);
            given(orgMemberRepository.findByUserIdAndOrgId(userId, orgId))
                    .willReturn(Optional.of(adminMember));

            timelineService.deleteTimeline(userId, orgId, timelineId);

            verify(timelineRepository, times(1)).delete(mockTimeline);
            verify(orgRepository, never()).findById(any()); // orgRepository not needed in delete
        }
    }
}