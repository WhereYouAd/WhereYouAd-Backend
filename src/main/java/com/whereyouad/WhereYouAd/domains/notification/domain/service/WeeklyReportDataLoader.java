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
        List<String> recipients = List.of();

        // 조직에 회원이 존재하면,
        if (!members.isEmpty()) {
            // recipients 를 수집 (이메일 알림 받는 회원 목록)
            List<Long> memberIds = members.stream().map(OrgMember::getId).toList();
            Map<Long, OrgMemberNotificationSetting> settingMap = settingRepository
                    .findByMembershipIdIn(memberIds).stream()
                    .collect(Collectors.toMap(OrgMemberNotificationSetting::getMembershipId, s -> s));

            // getEmail() 호출을 트랜잭션 안에서 완료하여 String 리스트로 구체화
            recipients = members.stream()
                    .filter(m -> isEmailReportEnabled(m, settingMap))
                    .map(m -> m.getUser().getEmail())
                    .filter(email -> email != null && !email.isBlank())
                    .toList();
        }

        boolean externalActive = notificationService.isExternalAlarmActive(orgId, NotificationType.REPORT);

        // 만약 이메일 수신 받는 회원이 없고, 외부 채널도 없으면
        if (recipients.isEmpty() && !externalActive) {
            // skip
            log.info("[WeeklyReport] 조직={} 이메일 수신자/외부 채널 모두 없음. 알림 skip", orgId);
            return Optional.empty();
        }

        List<MetricFact> thisWeekMetrics = List.of();
        List<MetricFact> prevWeekMetrics = List.of();

        // 만약 이메일 수신을 받는 회원이 존재하면
        if (!recipients.isEmpty()) {
            // 이번주 성과 데이터 수집
            thisWeekMetrics = metricFactRepository.findAllByDateRangeAndOrgForAiAnalysis(thisStart, thisEnd, orgId);

            // 이번주 데이터가 없으면 skip
            if (thisWeekMetrics.isEmpty()) {
                log.info("[WeeklyReport] 조직={} 이번 주 광고 데이터 없음. 이메일 리포트만 skip", orgId);
            } else {
                // 저번주 데이터가 없는 경우에는 PromptBuilder 에서 자동으로 0 으로 파싱하므로 빈 데이터 방어 X
                prevWeekMetrics = metricFactRepository.findAllByDateRangeAndOrgForAiAnalysis(prevStart, prevEnd, orgId);
            }
        }

        return Optional.of(new WeeklyReportData(org.getName(), recipients, thisWeekMetrics, prevWeekMetrics));
    }

    private boolean isEmailReportEnabled(OrgMember member, Map<Long, OrgMemberNotificationSetting> settingMap) {
        OrgMemberNotificationSetting setting = settingMap.get(member.getId());
        if (setting == null) return false;
        return setting.isMasterEnabled() && setting.isEmailEnabled() && setting.isAlertReport();
    }
}
