package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.WeeklyReportResponse;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.WeeklyReportData;
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

        // DB 트랜잭션 없는 상태에서 외부 호출 — 커넥션 풀 점유 없음
        WeeklyReportResponse.WeeklyAnalysisResponse analysis = openApiService.generateWeeklyReport(
                data.orgName(), null, null,
                thisWeekStart, thisWeekEnd, prevWeekStart, prevWeekEnd,
                data.thisWeekMetrics(), data.prevWeekMetrics());

        mailService.sendWeeklyReport(data.recipients(), analysis, data.orgName(), thisWeekStart, thisWeekEnd);
        log.info("[WeeklyReport] 조직={} 리포트 발송 완료. 수신자={}명", orgId, data.recipients().size());
    }
}
