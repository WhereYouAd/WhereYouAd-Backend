package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.exception.NotificationException;
import com.whereyouad.WhereYouAd.domains.notification.exception.code.NotificationErrorCode;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgMemberNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.global.utils.cursor.CursorUtil;
import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.domains.notification.exception.NotificationException;
import com.whereyouad.WhereYouAd.domains.notification.exception.code.NotificationErrorCode;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgNotificationSettingRepository;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.discord.DiscordWebhookClient;
import com.whereyouad.WhereYouAd.infrastructure.client.slack.SlackWebhookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final OrgMemberRepository orgMemberRepository;
    private final OrgMemberNotificationSettingRepository memberSettingRepository;
    private final OrgNotificationSettingRepository orgSettingRepository;

    // 현재 내 알림 설정 조회
    @Override
    public NotificationResponse.MySettings getMySettings(Long userId, Long orgId) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        OrgNotificationSetting orgSetting = orgSettingRepository.findById(orgId).orElse(null);
        return NotificationConverter.toMySettings(setting, orgSetting);
    }

    // 전체 알림 설정 변경 메서드
    @Override
    public void updateMaster(Long userId, Long orgId, NotificationRequest.UpdateMaster request) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        setting.updateMaster(request.isMasterEnabled());
    }

    // 채널별 알림 설정 메서드
    @Override
    public void updateChannels(Long userId, Long orgId, NotificationRequest.UpdateChannels request) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        setting.updateChannels(request.isBrowserPushEnabled(), request.isEmailEnabled(), request.isSlackEnabled(), request.isDiscordEnabled());

        // ADMIN인 경우 webhook url도 변경 가능
        // Optional이 null -> 필드 미전송(변경 없음), Optional.empty() -> 명시적 null(URL 삭제)
        if (member.getRole() == OrgRole.ADMIN) {
            boolean hasWebhookUpdate = request.slackWebhookUrl() != null || request.discordWebhookUrl() != null;

            // url 필드가 요청에 포함된 경우만 업데이트
            if (hasWebhookUpdate) {
                OrgNotificationSetting orgSetting = findOrCreateOrgSetting(member.getOrganization());
                if (request.slackWebhookUrl() != null) {
                    orgSetting.updateSlackWebhookUrl(request.slackWebhookUrl().orElse(null));
                }
                if (request.discordWebhookUrl() != null) {
                    orgSetting.updateDiscordWebhookUrl(request.discordWebhookUrl().orElse(null));
                }
            }
        }
    }

    // 알림 기준 설정 메서드(예산 소진, 클릭 수 급증 등)
    @Override
    public void updateAlerts(Long userId, Long orgId, NotificationRequest.UpdateAlerts request) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        setting.updateAlerts(request.alertBudget50(), request.alertBudget80(), request.alertBudget100(), request.alertRapidClicks());
    }

    @Transactional(readOnly = true)
    @Override
    public NotificationResponse.MemberSettingList getMemberSettings(Long userId, Long orgId, String encodedCursor, Integer size) {
        // ADMIN만 접근 가능
        OrgMember requester = findMember(userId, orgId);
        requireAdmin(requester);

        // 커서 디코딩 및 페이지 사이즈 결정 (기본 20)
        int pageSize = (size != null && size > 0) ? size : 20;
        Long cursor = (encodedCursor != null && !encodedCursor.isBlank()) ? CursorUtil.decodeToId(encodedCursor) : null;

        // 커서 기반으로 현재 페이지 멤버 조회
        Slice<OrgMember> slice = orgMemberRepository.findByOrganizationIdWithCursor(
                orgId, UserStatus.ACTIVE, cursor, PageRequest.of(0, pageSize));

        // 현재 페이지 멤버 ID로 알림 설정 일괄 조회
        List<Long> memberIds = slice.getContent().stream().map(OrgMember::getId).toList();
        Map<Long, OrgMemberNotificationSetting> settingsMap = memberSettingRepository.findByMembershipIdIn(memberIds)
                .stream()
                .collect(Collectors.toMap(OrgMemberNotificationSetting::getMembershipId, s -> s));

        // 설정이 없는 멤버는 수신 중(true)으로 간주
        List<NotificationResponse.MemberSetting> members = slice.getContent().stream()
                .map(m -> {
                    OrgMemberNotificationSetting s = settingsMap.get(m.getId());
                    boolean isReceive = s == null || s.isMasterEnabled();
                    return NotificationConverter.toMemberSetting(m, isReceive);
                })
                .toList();

        // 다음 페이지가 있으면 마지막 멤버 ID를 커서로 인코딩
        String nextCursor = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            Long lastId = slice.getContent().get(slice.getContent().size() - 1).getId();
            nextCursor = CursorUtil.encode(lastId);
        }

        return new NotificationResponse.MemberSettingList(slice.hasNext(), nextCursor, members);
    }

    // 멤버별 알림 설정 변경 메서드
    @Override
    public void updateMemberSettings(Long userId, Long orgId, NotificationRequest.BulkUpdateMembers request) {
        OrgMember requester = findMember(userId, orgId);
        requireAdmin(requester);

        for (NotificationRequest.UpdateMemberReceive update : request.members()) {
            OrgMember target = orgMemberRepository.findById(update.membershipId())
                    .filter(m -> m.getOrganization().getId().equals(orgId))
                    .orElseThrow(() -> new NotificationException(NotificationErrorCode.MEMBER_NOT_FOUND));
            OrgMemberNotificationSetting setting = findOrCreateSetting(target);
            setting.updateMaster(update.isReceive());
        }
    }

    // orgMember 조회 내부 메서드
    private OrgMember findMember(Long userId, Long orgId) {
        return orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.MEMBER_NOT_FOUND));
    }

    // 멤버의 알림 설정 조회(없으면 기본값으로 create)
    private OrgMemberNotificationSetting findOrCreateSetting(OrgMember member) {
        return memberSettingRepository.findById(member.getId())
                .orElseGet(() -> memberSettingRepository.save(
                        NotificationConverter.toDefaultMemberSetting(member)
                ));
    }

    // 조직의 알림 설정 조회(없으면 기본값으로 create)
    private OrgNotificationSetting findOrCreateOrgSetting(Organization organization) {
        return orgSettingRepository.findById(organization.getId())
                .orElseGet(() -> orgSettingRepository.save(
                        NotificationConverter.toDefaultOrgSetting(organization)
                ));
    }

    // ADMIN 권한 확인 메서드
    private void requireAdmin(OrgMember member) {
        if (member.getRole() != OrgRole.ADMIN) {
            throw new NotificationException(NotificationErrorCode.FORBIDDEN);
        }
    }

    private final OrgNotificationSettingRepository settingRepository;
    private final DiscordWebhookClient discordClient;
    private final SlackWebhookClient slackClient;
    private final AESUtil aesUtil;

    // 디스코드 / 슬랙 알림 전송 메서드
    // 조직 내에 웹훅 URL 이 설정되어 있는 경우 일괄 전송
    @Override
    @Transactional(readOnly = true)
    public void sendApiAlarmToOrg(Long orgId, String title, String message) {
        OrgNotificationSetting setting = settingRepository.findById(orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.ORG_NOTIFICATION_SETTING_NOT_FOUND));

        if (!setting.hasSlack() && !setting.hasDiscord()) {
            throw new NotificationException(NotificationErrorCode.NO_CHANNEL_CONFIGURED);
        }

        if (setting.hasSlack()) {
            dispatch(DeliveryChannel.SLACK, setting.getSlackWebhookUrl(), orgId,
                    uri -> slackClient.send(uri, NotificationConverter.toSlackMessage(title, message)));
        }

        if (setting.hasDiscord()) {
            dispatch(DeliveryChannel.DISCORD, setting.getDiscordWebhookUrl(), orgId,
                    uri -> discordClient.send(uri, NotificationConverter.toDiscordMessage(title, message)));
        }
    }

    // 웹훅 URL 복호화 -> 알림 발송 -> 채널별 실패 격리 -> 로깅
    private void dispatch(DeliveryChannel channel, String encryptedUrl, Long orgId, Consumer<URI> sendAction) {
        try {
            String url = new String(aesUtil.decryptAES(encryptedUrl), StandardCharsets.UTF_8).trim();
            sendAction.accept(URI.create(url));
            log.info("[알림 발송 성공] channel={}, orgId={}", channel, orgId);
        } catch (Exception e) {
            log.error("[알림 발송 실패] channel={}, orgId={}, reason={}", channel, orgId, e.getMessage(), e);
        }
    }
}
