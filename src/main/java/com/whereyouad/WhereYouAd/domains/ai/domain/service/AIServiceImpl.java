package com.whereyouad.WhereYouAd.domains.ai.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.request.AIRequest;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;
import com.whereyouad.WhereYouAd.domains.ai.application.mapper.AIConverter;
import com.whereyouad.WhereYouAd.domains.ai.domain.constant.AIStatus;
import com.whereyouad.WhereYouAd.domains.ai.exception.AIHandler;
import com.whereyouad.WhereYouAd.domains.ai.exception.code.AIErrorCode;
import com.whereyouad.WhereYouAd.domains.ai.persistence.entity.AIInsightReport;
import com.whereyouad.WhereYouAd.domains.ai.persistence.repository.AIInsightReportRepository;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final MetricFactRepository metricFactRepository;
    private final AIInsightReportRepository reportRepository;
    private final AIAsyncService aiAsyncService;
    private final ObjectMapper objectMapper;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;

    @Override
    @Transactional
    public Long requestAnalysis(Long userId, Long orgId, AIRequest.PeriodRequest request) {

        // 1. 조직 존재 여부 확인
        orgRepository.findById(orgId)
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

        // 4. 해당 조직의 데이터 존재 여부 사전 확인
        boolean hasData = metricFactRepository.existsByTimeBucketBetweenAndOrg(start, end, orgId);
        if (!hasData) {
            throw new AIHandler(AIErrorCode.NO_METRIC_DATA);
        }

        // 5. AIInsightReport PENDING 상태로 DB 저장
        AIInsightReport report = AIConverter.toAIInsightConverter(start, end);
        reportRepository.save(report);

        log.info("[AIServiceImpl] 분석 요청 접수. reportId={}, orgId={}, 기간={} ~ {}",
                report.getId(), orgId, request.startDate(), request.endDate());

        // 6. 트랜잭션 커밋 완료 후 비동기 분석 트리거
        // afterCommit(): PENDING 레코드가 DB에 확정된 뒤에 @Async 실행
        final Long reportId = report.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiAsyncService.analyzeAsync(reportId, orgId, request.startDate(), request.endDate());
            }
        });

        // 7. reportId 반환 (202 Accepted)
        return report.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse.ReportStatusResponse getReport(Long userId, Long orgId, Long reportId) {

        // 1. 조직 존재 여부 확인
        orgRepository.findById(orgId)
                .orElseThrow(() -> new AIHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 2. 유저가 해당 조직의 멤버인지 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new AIHandler(AIErrorCode.AI_ACCESS_FORBIDDEN));

        // 3. Id에 해당하는 분석 리포트가 없는 경우
        AIInsightReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AIHandler(AIErrorCode.REPORT_NOT_FOUND));

        // PENDING / FAILED 일 때는 result = null
        if (report.getStatus() != AIStatus.SUCCESS || report.getPayloadJson() == null) {
            return new AIResponse.ReportStatusResponse(
                    report.getId(),
                    report.getStatus().name(),
                    null);
        }

        // SUCCESS: payloadJson -> AnalysisResponse 역직렬화
        try {
            AIResponse.AnalysisResponse result = objectMapper.readValue(report.getPayloadJson(),
                    AIResponse.AnalysisResponse.class);
            return new AIResponse.ReportStatusResponse(
                    report.getId(),
                    report.getStatus().name(),
                    result);
        } catch (Exception e) {
            log.error("[AIServiceImpl] payloadJson 역직렬화 실패. reportId={}", reportId, e);
            throw new AIHandler(AIErrorCode.AI_CALL_FAILED);
        }
    }
}
