package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.WeeklyReportData;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgMemberNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportDataLoader {

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgMemberNotificationSettingRepository settingRepository;
    private final MetricFactRepository metricFactRepository;
    private final NotificationService notificationService;

    /**
     * 단일 조직의 주간 리포트 발송에 필요한 데이터를 트랜잭션 안에서 완전히 구체화한다.
     * 반환 후에는 DB 세션이 닫히므로, 호출자는 반환된 값만 사용해야 한다.
     */
    @Transactional(readOnly = true)
    public Optional<WeeklyReportData> load(Long orgId,
                                           LocalDateTime thisStart, LocalDateTime thisEnd,
                                           LocalDateTime prevStart, LocalDateTime prevEnd) {
        Organization org = orgRepository.findById(orgId).orElse(null);
        if (org == null) {
            log.warn("[WeeklyReport] 조직={} 조회 실패. 건너뜀", orgId);
            return Optional.empty();
        }

        // findOrgMemberByOrg 는 join fetch om.user 로 User 를 함께 로딩
        List<OrgMember> members = orgMemberRepository.findOrgMemberByOrg(org);
        if (members.isEmpty()) return Optional.empty();

        List<Long> memberIds = members.stream().map(OrgMember::getId).toList();
        Map<Long, OrgMemberNotificationSetting> settingMap = settingRepository
                .findByMembershipIdIn(memberIds).stream()
                .collect(Collectors.toMap(OrgMemberNotificationSetting::getMembershipId, s -> s));

        // getEmail() 호출을 트랜잭션 안에서 완료하여 String 리스트로 구체화
        List<String> recipients = members.stream()
                .filter(m -> isEmailReportEnabled(m, settingMap))
                .map(m -> m.getUser().getEmail())
                .filter(email -> email != null && !email.isBlank())
                .toList();

        if (recipients.isEmpty()) {
            // 알림 대상자가 없는 경우 검증에서 외부 채널 알림도 미수신인지 검증 로직 추가
            if (!notificationService.isExternalAlarmActive(orgId, NotificationType.REPORT)) {
                log.info("[WeeklyReport] 조직={} 이메일 수신자/외부 채널 모두 없음. 건너뜀", orgId);
                return Optional.empty();
            }
            log.info("[WeeklyReport] 조직={} 이메일 수신자는 없으나 외부 채널 수신 활성화됨. 알림 전송 진행", orgId);
        }

        // findAllByDateRangeAndOrgForAiAnalysis 는 adContent/adGroup/adCampaign 을 JOIN FETCH
        List<MetricFact> thisWeekMetrics = metricFactRepository
                .findAllByDateRangeAndOrgForAiAnalysis(thisStart, thisEnd, orgId);

        if (thisWeekMetrics.isEmpty()) {
            log.info("[WeeklyReport] 조직={} 이번 주 광고 데이터 없음. 건너뜀", orgId);
            return Optional.empty();
        }

        List<MetricFact> prevWeekMetrics = metricFactRepository
                .findAllByDateRangeAndOrgForAiAnalysis(prevStart, prevEnd, orgId);

        return Optional.of(new WeeklyReportData(org.getName(), recipients, thisWeekMetrics, prevWeekMetrics));
    }

    private boolean isEmailReportEnabled(OrgMember member, Map<Long, OrgMemberNotificationSetting> settingMap) {
        OrgMemberNotificationSetting setting = settingMap.get(member.getId());
        if (setting == null) return false;
        return setting.isMasterEnabled() && setting.isEmailEnabled() && setting.isAlertReport();
    }
}
