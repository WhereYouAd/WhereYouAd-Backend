package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.WeeklyReportResponse;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgMemberNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.mail.AIMailService;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.service.OpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportNotificationService {

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgMemberNotificationSettingRepository settingRepository;
    private final MetricFactRepository metricFactRepository;
    private final OpenApiService openApiService;
    private final AIMailService mailService;

    @Transactional(readOnly = true)
    public void sendWeeklyReportForOrg(Long orgId) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));
        LocalDate today = LocalDate.now();
        LocalDate thisWeekEnd = today.minusDays(1);
        LocalDate thisWeekStart = today.minusDays(7);
        LocalDate prevWeekEnd = today.minusDays(8);
        LocalDate prevWeekStart = today.minusDays(14);
        processOrg(org, thisWeekStart, thisWeekEnd, prevWeekStart, prevWeekEnd);
    }

    @Transactional(readOnly = true)
    public void sendWeeklyReports() {
        LocalDate today = LocalDate.now();
        // 직전 7일 (지난 주 월~일)
        LocalDate thisWeekEnd = today.minusDays(1);
        LocalDate thisWeekStart = today.minusDays(7);
        // 그 이전 7일 (2주 전 월~일)
        LocalDate prevWeekEnd = today.minusDays(8);
        LocalDate prevWeekStart = today.minusDays(14);

        List<Organization> activeOrgs = orgRepository.findAllByStatus(OrgStatus.ACTIVE);
        log.info("[WeeklyReport] 주간 리포트 발송 시작. 대상 조직 수={}", activeOrgs.size());

        for (Organization org : activeOrgs) {
            try {
                processOrg(org, thisWeekStart, thisWeekEnd, prevWeekStart, prevWeekEnd);
            } catch (Exception e) {
                log.error("[WeeklyReport] 조직={} 리포트 처리 실패: {}", org.getId(), e.getMessage());
            }
        }

        log.info("[WeeklyReport] 주간 리포트 발송 완료.");
    }

    private void processOrg(Organization org,
                             LocalDate thisWeekStart, LocalDate thisWeekEnd,
                             LocalDate prevWeekStart, LocalDate prevWeekEnd) {

        // 1. 이메일 수신 대상 조회
        List<OrgMember> members = orgMemberRepository.findOrgMemberByOrg(org);
        if (members.isEmpty()) return;

        List<Long> memberIds = members.stream().map(OrgMember::getId).toList();
        Map<Long, OrgMemberNotificationSetting> settingMap = settingRepository
                .findByMembershipIdIn(memberIds).stream()
                .collect(Collectors.toMap(OrgMemberNotificationSetting::getMembershipId, s -> s));

        List<String> recipients = members.stream()
                .filter(m -> isEmailReportEnabled(m, settingMap))
                .map(m -> m.getUser().getEmail())
                .filter(email -> email != null && !email.isBlank())
                .toList();

        if (recipients.isEmpty()) {
            log.info("[WeeklyReport] 조직={} 이메일 수신자 없음. 건너뜀", org.getId());
            return;
        }

        // 2. MetricFact 조회
        LocalDateTime thisStart = thisWeekStart.atStartOfDay();
        LocalDateTime thisEnd = thisWeekEnd.atTime(23, 59, 59);
        List<MetricFact> thisWeekMetrics = metricFactRepository
                .findAllByDateRangeAndOrgForAiAnalysis(thisStart, thisEnd, org.getId());

        if (thisWeekMetrics.isEmpty()) {
            log.info("[WeeklyReport] 조직={} 이번 주 광고 데이터 없음. 건너뜀", org.getId());
            return;
        }

        LocalDateTime prevStart = prevWeekStart.atStartOfDay();
        LocalDateTime prevEnd = prevWeekEnd.atTime(23, 59, 59);
        List<MetricFact> prevWeekMetrics = metricFactRepository
                .findAllByDateRangeAndOrgForAiAnalysis(prevStart, prevEnd, org.getId());

        // 3. AI 분석 생성
        WeeklyReportResponse.WeeklyAnalysisResponse analysis = openApiService.generateWeeklyReport(
                org.getName(), null, null,
                thisWeekStart, thisWeekEnd, prevWeekStart, prevWeekEnd,
                thisWeekMetrics, prevWeekMetrics);

        // 4. 이메일 발송
        mailService.sendWeeklyReport(recipients, analysis, org.getName(), thisWeekStart, thisWeekEnd);
        log.info("[WeeklyReport] 조직={} 리포트 발송 완료. 수신자={}명", org.getId(), recipients.size());
    }

    private boolean isEmailReportEnabled(OrgMember member, Map<Long, OrgMemberNotificationSetting> settingMap) {
        OrgMemberNotificationSetting setting = settingMap.get(member.getId());
        if (setting == null) return false; // 기본값: alertReport=false (명시적 opt-in 필요)
        return setting.isMasterEnabled() && setting.isEmailEnabled() && setting.isAlertReport();
    }
}
