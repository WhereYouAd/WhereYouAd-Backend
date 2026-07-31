package com.whereyouad.WhereYouAd.domains.ai.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.request.AIRequest;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;
import com.whereyouad.WhereYouAd.domains.ai.application.mapper.AIConverter;
import com.whereyouad.WhereYouAd.domains.ai.domain.constant.AIStatus;
import com.whereyouad.WhereYouAd.domains.ai.exception.AIHandler;
import com.whereyouad.WhereYouAd.domains.ai.exception.code.AIErrorCode;
import com.whereyouad.WhereYouAd.domains.ai.persistence.entity.AIInsightReport;
import com.whereyouad.WhereYouAd.domains.ai.persistence.repository.AIInsightReportRepository;
import com.whereyouad.WhereYouAd.domains.ai.persistence.repository.projection.AIReportSummaryProjection;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.global.utils.cursor.CursorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private static final int DEFAULT_REPORT_LIST_SIZE = 20;
    private static final int MAX_REPORT_LIST_SIZE = 50;
    private static final DateTimeFormatter REPORT_TITLE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final MetricFactRepository metricFactRepository;
    private final AIInsightReportRepository reportRepository;
    private final AIAsyncService aiAsyncService;
    private final ObjectMapper objectMapper;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;

    @Override
    @Transactional
    public String requestAnalysis(Long userId, Long orgId, AIRequest.AnalysisRequest request) {

        // 1. 조직 존재 여부 확인
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new AIHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 2. 유저가 해당 조직의 멤버인지 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new AIHandler(AIErrorCode.AI_ACCESS_FORBIDDEN));

        // 3. 날짜 유효성 검사
        if (request.startDate().isAfter(request.endDate())) {
            throw new AIHandler(AIErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime start = request.startDate().atStartOfDay();
        LocalDateTime end = request.endDate().atTime(23, 59, 59);

        // 4. 해당 조직의(또는 플랫폼) 데이터 존재 여부 사전 확인
        boolean hasData;
        if ("ALL".equalsIgnoreCase(request.provider())) {
            hasData = metricFactRepository.existsByTimeBucketBetweenAndOrg(start, end, orgId);
        } else {
            hasData = metricFactRepository.existsByTimeBucketBetweenAndOrgAndProvider(
                    start, end, orgId, Provider.valueOf(request.provider().toUpperCase()));
        }

        if (!hasData) {
            throw new AIHandler(AIErrorCode.NO_METRIC_DATA);
        }

        // 5. AIInsightReport PENDING 상태로 DB 저장
        AIInsightReport report = AIConverter.toAIInsightConverter(start, end, organization, request.provider());
        reportRepository.save(report);

        log.info("[AIServiceImpl] 분석 요청 접수. reportId={}, orgId={}, provider={}, 기간={} ~ {}",
                report.getId(), orgId, request.provider(), request.startDate(), request.endDate());

        // 6. 트랜잭션 커밋 완료 후 비동기 분석 트리거
        // afterCommit(): PENDING 레코드가 DB에 확정된 뒤에 @Async 실행
        final Long reportId = report.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiAsyncService.analyzeAsync(reportId, orgId, request.provider(), request.startDate(), request.endDate());
            }
        });

        // 7. accessToken 반환 (202 Accepted)
        return report.getAccessToken();
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse.ReportStatusResponse getReportByAccessToken(Long userId, String accessToken) {

        // accessToken에 해당하는 분석 리포트가 없는 경우
        AIInsightReport report = reportRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new AIHandler(AIErrorCode.REPORT_NOT_FOUND));

        if (!report.isShared()) {
            if (userId == null) {
                throw new AIHandler(AIErrorCode.AI_ACCESS_FORBIDDEN);
            }
            Long orgId = report.getOrganization().getId();

            // 1. 조직 존재 여부 확인
            orgRepository.findById(orgId)
                    .orElseThrow(() -> new AIHandler(OrgErrorCode.ORG_NOT_FOUND));

            // 2. 유저가 해당 조직의 멤버인지 검증
            orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                    .orElseThrow(() -> new AIHandler(AIErrorCode.AI_ACCESS_FORBIDDEN));
        }

        // PENDING / FAILED 일 때는 result = null
        if (report.getStatus() != AIStatus.SUCCESS || report.getPayloadJson() == null) {
            return new AIResponse.ReportStatusResponse(
                    report.getAccessToken(),
                    report.getStatus().name(),
                    null);
        }

        // SUCCESS: payloadJson -> AnalysisResponse 역직렬화
        try {
            AIResponse.AnalysisResponse result = objectMapper.readValue(report.getPayloadJson(),
                    AIResponse.AnalysisResponse.class);
            return new AIResponse.ReportStatusResponse(
                    report.getAccessToken(),
                    report.getStatus().name(),
                    result);
        } catch (Exception e) {
            log.error("[AIServiceImpl] payloadJson 역직렬화 실패. report.id={}", report.getId(), e);
            throw new AIHandler(AIErrorCode.AI_CALL_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse.ReportListResponse getReportSummaries(
            Long userId, Long orgId, String reportType, String encodedCursor, Integer size) {

        orgRepository.findById(orgId)
                .orElseThrow(() -> new AIHandler(OrgErrorCode.ORG_NOT_FOUND));

        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new AIHandler(AIErrorCode.AI_ACCESS_FORBIDDEN));

        int pageSize = resolvePageSize(size);
        String normalizedReportType = normalizeReportType(reportType);
        Long cursor = encodedCursor != null && !encodedCursor.isBlank()
                ? CursorUtil.decodeToId(encodedCursor)
                : null;

        Slice<AIReportSummaryProjection> reportSlice = reportRepository.findSummariesByOrganizationId(
                orgId, normalizedReportType, cursor, PageRequest.of(0, pageSize));

        List<AIResponse.ReportSummaryResponse> reports = reportSlice.getContent().stream()
                .map(this::toReportSummaryResponse)
                .toList();

        String nextCursor = null;
        if (reportSlice.hasNext() && !reportSlice.getContent().isEmpty()) {
            Long lastReportId = reportSlice.getContent()
                    .get(reportSlice.getContent().size() - 1)
                    .getReportId();
            nextCursor = CursorUtil.encode(lastReportId);
        }

        return new AIResponse.ReportListResponse(reportSlice.hasNext(), nextCursor, reports);
    }

    @Override
    @Transactional
    public void updateShareStatus(Long userId, String accessToken, boolean isShared) {
        AIInsightReport report = reportRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new AIHandler(AIErrorCode.REPORT_NOT_FOUND));

        Long orgId = report.getOrganization().getId();

        // 1. 조직 존재 여부 확인
        orgRepository.findById(orgId)
                .orElseThrow(() -> new AIHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 2. 유저가 해당 조직의 멤버인지 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new AIHandler(AIErrorCode.AI_ACCESS_FORBIDDEN));

        report.updateIsShared(isShared);
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_REPORT_LIST_SIZE;
        }
        return Math.min(size, MAX_REPORT_LIST_SIZE);
    }

    private String normalizeReportType(String reportType) {
        if (reportType == null || reportType.isBlank()) {
            return null;
        }
        return reportType.trim().toUpperCase(Locale.ROOT);
    }

    private AIResponse.ReportSummaryResponse toReportSummaryResponse(AIReportSummaryProjection report) {
        return new AIResponse.ReportSummaryResponse(
                report.getReportId(),
                report.getAccessToken(),
                buildReportTitle(report),
                report.getStatus().name(),
                report.getShared(),
                report.getCreatedAt()
        );
    }

    private String buildReportTitle(AIReportSummaryProjection report) {
        String reportTarget = "ALL".equalsIgnoreCase(report.getReportType())
                ? "전체"
                : report.getReportType().toUpperCase(Locale.ROOT);

        return "%s ~ %s %s 광고 분석".formatted(
                report.getPeriodStart().toLocalDate().format(REPORT_TITLE_DATE_FORMATTER),
                report.getPeriodEnd().toLocalDate().format(REPORT_TITLE_DATE_FORMATTER),
                reportTarget
        );
    }
}
