package com.whereyouad.WhereYouAd.domains.timeline.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.timeline.exception.TimelineException;
import com.whereyouad.WhereYouAd.domains.timeline.exception.code.TimelineErrorCode;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.repository.TimelineRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.service.OpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineAsyncServiceImpl implements TimelineAsyncService {

    private final TimelineRepository timelineRepository;
    private final MetricFactRepository metricFactRepository;
    private final OpenApiService openApiService;

    @Override
    @Async
    @Transactional
    public void summarizeAsync(Long timelineId, Long orgId) {
        Timeline timeline = timelineRepository.findById(timelineId)
                .orElseThrow(() -> new TimelineException(TimelineErrorCode.TIMELINE_NOT_FOUND));

        // 타임라인에 해당하는 Metric_fact 데이터 불러오기
        List<MetricFact> facts = metricFactRepository.findByOrgAndPeriodAndGrain(
                orgId,
                timeline.getStartDate().atStartOfDay(),
                timeline.getEndDate().plusDays(1).atStartOfDay(),
                Grain.DAILY
        );

        try {
            // 타임라인 AI요약 생성 요청
            String summary = openApiService.generateTimelineSummary(timeline, facts);
            timeline.updateSummary(summary);
        } catch (Exception e) {
            log.error("[Timeline AI 요약 실패] timelineId={}, error={}", timelineId, e.getMessage());
        }
    }
}
