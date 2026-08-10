package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.BotClickSummaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        List<BotClickSummaryData> summaries = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long orgId = (Long) row[0];
            List<BotClickSummaryData.TopAd> topAds = clickLogRepository
                    .findTopSuspectAdsByOrg(orgId, start, end, PageRequest.of(0, TOP_AD_LIMIT))
                    .stream()
                    .map(adRow -> new BotClickSummaryData.TopAd(
                            adRow[0] != null ? (String) adRow[0] : "알 수 없는 광고",
                            (Long) adRow[1]))
                    .toList();

            summaries.add(new BotClickSummaryData(
                    orgId,
                    (String) row[1],
                    (Long) row[2],
                    (Long) row[3],
                    (Long) row[4],
                    topAds));
        }
        return summaries;
    }
}
