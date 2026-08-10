package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.BotClickSummaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BotClickSummaryDataLoader {

    private final ClickLogRepository clickLogRepository;

    private static final int TOP_AD_LIMIT = 3;

    /**
     * 대상 일자의 조직별 봇 클릭 요약을 트랜잭션 안에서 완전히 구체화한다.
     * 봇 클릭이 0건인 조직은 결과에 포함되지 않는다.
     */
    @Transactional(readOnly = true)
    public List<BotClickSummaryData> load(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<Object[]> rows = clickLogRepository.summarizeSuspectClicksByOrg(start, end);
        Map<Long, List<BotClickSummaryData.TopAd>> topAdsByOrg = loadTopAdsByOrg(start, end);

        List<BotClickSummaryData> summaries = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long orgId = (Long) row[0];
            summaries.add(new BotClickSummaryData(
                    orgId,
                    (String) row[1],
                    (Long) row[2],
                    (Long) row[3],
                    (Long) row[4],
                    topAdsByOrg.getOrDefault(orgId, List.of())));
        }
        return summaries;
    }

    // 전 조직 광고별 집계를 쿼리 1방에 조회 후 조직별 상위 TOP_AD_LIMIT개만 취한다 (조직, 클릭수 내림차순 정렬 전제)
    private Map<Long, List<BotClickSummaryData.TopAd>> loadTopAdsByOrg(LocalDateTime start, LocalDateTime end) {
        Map<Long, List<BotClickSummaryData.TopAd>> topAdsByOrg = new HashMap<>();
        for (Object[] adRow : clickLogRepository.summarizeSuspectAdClicksByOrg(start, end)) {
            List<BotClickSummaryData.TopAd> topAds = topAdsByOrg.computeIfAbsent((Long) adRow[0], key -> new ArrayList<>());
            if (topAds.size() < TOP_AD_LIMIT) {
                topAds.add(new BotClickSummaryData.TopAd(
                        adRow[1] != null ? (String) adRow[1] : "알 수 없는 광고",
                        (Long) adRow[2]));
            }
        }
        return topAdsByOrg;
    }
}
