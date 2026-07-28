package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.WeeklyReportResponse;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.WeeklyReportData;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.mail.AIMailService;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.service.OpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportNotificationService {

    private final OrgRepository orgRepository;
    private final WeeklyReportDataLoader dataLoader;
    private final OpenApiService openApiService;
    private final AIMailService mailService;
    private final NotificationService notificationService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M월 d일");

    // 외부 채널에는 리포트 본문을 담지 않고 안내 문구만 발송
    // 조직 내 이메일로 리포트 수신하는 회원이 존재할 경우 안내 문구
    private static final String CHANNEL_MESSAGE_WITH_EMAIL =
            "오늘은 주간 리포트 작성일 입니다! 자세한 내용은 이메일 또는 WhereYouAd 대시보드에서 확인해주세요.";
    // 조직 내 이메일로 리포트 수신하는 회원이 존재하지 않을 경우 안내 문구
    private static final String CHANNEL_MESSAGE_WITHOUT_EMAIL =
            "오늘은 주간 리포트 작성일 입니다! WhereYouAd 대시보드 내용을 기반으로 리포트를 작성해보세요.";

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendWeeklyReportForOrg(Long orgId) {
        orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        LocalDate today = LocalDate.now();
        processOrg(orgId, today.minusDays(7), today.minusDays(1), today.minusDays(14), today.minusDays(8));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendWeeklyReports() {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekEnd   = today.minusDays(1);
        LocalDate thisWeekStart = today.minusDays(7);
        LocalDate prevWeekEnd   = today.minusDays(8);
        LocalDate prevWeekStart = today.minusDays(14);

        List<Long> orgIds = orgRepository.findAllByStatus(OrgStatus.ACTIVE)
                .stream().map(Organization::getId).toList();
        log.info("[WeeklyReport] 주간 리포트 발송 시작. 대상 조직 수={}", orgIds.size());

        for (Long orgId : orgIds) {
            try {
                processOrg(orgId, thisWeekStart, thisWeekEnd, prevWeekStart, prevWeekEnd);
            } catch (Exception e) {
                log.error("[WeeklyReport] 조직={} 리포트 처리 실패: {}", orgId, e.getMessage());
            }
        }

        log.info("[WeeklyReport] 주간 리포트 발송 완료.");
    }

    private void processOrg(Long orgId,
                             LocalDate thisWeekStart, LocalDate thisWeekEnd,
                             LocalDate prevWeekStart, LocalDate prevWeekEnd) {

        LocalDateTime thisStart = thisWeekStart.atStartOfDay();
        LocalDateTime thisEnd   = thisWeekEnd.plusDays(1).atStartOfDay();
        LocalDateTime prevStart = prevWeekStart.atStartOfDay();
        LocalDateTime prevEnd   = prevWeekEnd.plusDays(1).atStartOfDay();

        // 읽기 전용 트랜잭션 안에서 모든 DB 데이터를 완전히 구체화
        Optional<WeeklyReportData> dataOpt = dataLoader.load(orgId, thisStart, thisEnd, prevStart, prevEnd);
        if (dataOpt.isEmpty()) {
            log.info("[WeeklyReport] 조직={} 처리 대상 없음. 건너뜀", orgId);
            return;
        }
        WeeklyReportData data = dataOpt.get();

        // 디스코드 / 슬랙 리포트 작성 알림 발송
        sendExternalChannelAlarm(orgId, data.orgName(), thisWeekStart, thisWeekEnd, !data.recipients().isEmpty());

        // 이메일 수신자가 0명이거나 이번주 데이터가 없으면 리포트를 쓸 수 없으므로 OpenAI 호출 자체를 건너뛴다.
        // (생성된 리포트는 DB에 저장되지 않고 메일 본문으로만 소비되기 때문)
        if (!data.recipients().isEmpty() && !data.thisWeekMetrics().isEmpty()) {
            // DB 트랜잭션 없는 상태에서 외부 호출 — 커넥션 풀 점유 없음
            WeeklyReportResponse.WeeklyAnalysisResponse analysis = openApiService.generateWeeklyReport(
                    data.orgName(), null, null,
                    thisWeekStart, thisWeekEnd, prevWeekStart, prevWeekEnd,
                    data.thisWeekMetrics(), data.prevWeekMetrics());

            mailService.sendWeeklyReport(data.recipients(), analysis, data.orgName(), thisWeekStart, thisWeekEnd);
            log.info("[WeeklyReport] 조직={} 이메일 리포트 발송 완료. 수신자={}명", orgId, data.recipients().size());
        }
    }

    // 조직에 연결된 디스코드 / 슬랙 채널로 "주간 리포트 작성 시간" 알림 발송 메서드
    // 리포트 본문은 이메일로만 전송, 외부 채널에는 안내 문구만 전송
    private void sendExternalChannelAlarm(Long orgId, String orgName,
                                          LocalDate weekStart, LocalDate weekEnd,
                                          boolean hasEmailRecipients) {
        try {
            String title = String.format("[%s] 주간 광고 리포트 (%s ~ %s)",
                    orgName, weekStart.format(DATE_FMT), weekEnd.format(DATE_FMT));

            // 이메일로 리포트를 수신하는 회원이 없으면 이메일 안내 문구를 뺀 메세지를 사용
            String message = hasEmailRecipients ? CHANNEL_MESSAGE_WITH_EMAIL : CHANNEL_MESSAGE_WITHOUT_EMAIL;

            // 실제 발송 조건 판정(웹훅 등록 여부 / 채널 수신 토글 ON / alertReport ON)과
            // 채널별 실패 격리는 NotificationService.sendApiAlarmToOrg() 내부 처리
            notificationService.sendApiAlarmToOrg(orgId, NotificationType.REPORT, title, message);
        } catch (Exception e) {
            log.error("[WeeklyReport] 조직={} 외부 채널(슬랙/디스코드) 리포트 알림 발송 실패: {}", orgId, e.getMessage(), e);
        }
    }
}
