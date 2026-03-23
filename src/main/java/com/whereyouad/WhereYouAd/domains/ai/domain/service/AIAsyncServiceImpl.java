package com.whereyouad.WhereYouAd.domains.ai.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;
import com.whereyouad.WhereYouAd.domains.ai.exception.AIHandler;
import com.whereyouad.WhereYouAd.domains.ai.exception.code.AIErrorCode;
import com.whereyouad.WhereYouAd.domains.ai.persistence.entity.AIInsightReport;
import com.whereyouad.WhereYouAd.domains.ai.persistence.repository.AIInsightReportRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.service.OpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAsyncServiceImpl implements AIAsyncService{

    private final MetricFactRepository metricFactRepository;
    private final AIInsightReportRepository reportRepository;
    private final OpenApiService openApiUtil;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void analyzeAsync(Long reportId, Long projectId, LocalDate startDate, LocalDate endDate) {
        log.info("[AIAsyncService] 비동기 분석 시작. reportId={}, projectId={}, 기간={} ~ {}",
                reportId, projectId, startDate, endDate);

        AIInsightReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> {
                    log.error("[AIAsyncService] AIInsightReport 를 찾을 수 없습니다. reportId={}", reportId);
                    return new AIHandler(AIErrorCode.REPORT_NOT_FOUND);
                });

        try {
            // 1. 해당 프로젝트의 기간 내 MetricFact 조회
            List<MetricFact> metrics = metricFactRepository.findAllByDateRangeAndProjectForAiAnalysis(
                    startDate.atStartOfDay(),
                    endDate.atTime(23, 59, 59),
                    projectId);

            // 2. OpenAI API 호출 -> AnalysisResponse
            AIResponse.AnalysisResponse analysisResponse = openApiUtil.generateAnalysis(startDate, endDate, metrics);

            // 3. 결과 JSON 직렬화 후 SUCCESS 업데이트
            String payloadJson = objectMapper.writeValueAsString(analysisResponse);
            report.updateSuccess(payloadJson);
            log.info("[AIAsyncService] 분석 완료. reportId={} → SUCCESS", reportId);

        } catch (AIHandler e) {
            log.error("[AIAsyncService] AI 분석 실패 (도메인 예외). reportId={}, error={}", reportId, e.getMessage());
            report.updateFailed();

        } catch (Exception e) {
            log.error("[AIAsyncService] AI 분석 중 예기치 않은 오류 발생. reportId={}", reportId, e);
            report.updateFailed();
        }
    }
}
