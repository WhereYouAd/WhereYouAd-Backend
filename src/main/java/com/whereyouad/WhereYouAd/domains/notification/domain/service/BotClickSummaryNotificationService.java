package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.BotClickSummaryData;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotClickSummaryNotificationService {

    private final BotClickSummaryDataLoader dataLoader;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M월 d일");

    // 발송은 트랜잭션 밖에서 - 외부 웹훅 호출이 DB 커넥션을 점유하지 않도록 (WeeklyReport 패턴)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendDailyBotSummaries() {
        // KST 기준 "어제" - clickedAt 저장 존(ClickWindowKeys.ZONE_ID)과 일치해야 경계가 정합
        LocalDate yesterday = LocalDate.now(ClickWindowKeys.ZONE_ID).minusDays(1);
        List<BotClickSummaryData> summaries = dataLoader.load(yesterday);

        // 봇 클릭 0건 조직은 집계 결과에 없으므로 자동 skip
        log.info("[봇클릭요약] 일일 요약 발송 시작. 대상 조직 수={}", summaries.size());

        for (BotClickSummaryData summary : summaries) {
            try {
                String title = String.format("[%s] 어제의 봇 클릭 요약 (%s)",
                        summary.orgName(), yesterday.format(DATE_FMT));
                notificationService.sendApiAlarmToOrg(
                        summary.orgId(), NotificationType.BOT_CLICKS, title, buildMessage(summary));
            } catch (Exception e) {
                // 조직별 발송 실패 격리
                log.error("[봇클릭요약] 조직={} 발송 실패", summary.orgId(), e);
            }
        }

        log.info("[봇클릭요약] 일일 요약 발송 완료.");
    }

    private String buildMessage(BotClickSummaryData summary) {
        String header = String.format("총 의심 클릭 %d회 · 유니크 IP %d개 · 영향받은 광고 %d개",
                summary.totalSuspectClicks(), summary.distinctIpCount(), summary.affectedAdCount());

        if (summary.topAds().isEmpty()) {
            return header;
        }
        String topAds = summary.topAds().stream()
                .map(topAd -> String.format("%s(%d회)", topAd.adName(), topAd.clickCount()))
                .collect(Collectors.joining(", "));
        return header + "\n상위 광고: " + topAds;
    }
}
