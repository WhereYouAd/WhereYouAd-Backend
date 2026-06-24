package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.exception.NotificationException;
import com.whereyouad.WhereYouAd.domains.notification.exception.code.NotificationErrorCode;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
public class OrgNotificationSettingServiceImpl implements OrgNotificationSettingService {

    private final OrgNotificationSettingRepository settingRepository;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final NotificationService notificationService;
    private final AESUtil aesUtil;

    // 조직 단위 디스코드 or 슬랙 채널 웹훅 URL 을 OrgNotificationSetting 엔티티로 UPSERT 저장
    @Override
    @Transactional
    public NotificationResponse.ChannelsListResponse upsertChannels(Long userId, Long orgId, NotificationRequest.ChannelSettingRequest request) {
        validateAdmin(userId, orgId);

        OrgNotificationSetting setting = settingRepository.findById(orgId) // 조직에 기존 OrgNotificationSetting 있으면 조회
                .orElseGet(() ->
                        OrgNotificationSetting.builder() // 기존 OrgNotificationSetting 없으면 생성
                        .organization(getOrganization(orgId))
                        .build()
                );

        // OrgNotificationSetting 에서 디스코드 / 슬랙 웹훅 URL 업데이트
        setting.updateChannel(
                encryptOrNull(request.slackWebhookUrl()),
                encryptOrNull(request.discordWebhookUrl())
        );

        settingRepository.save(setting);

        return NotificationConverter.toChannelsListResponse(setting);
    }

    // 현재 조직에 연동된 외부 알림 채널(디스코드 / 슬랙) 이 어떤 것이 있는지 조회
    // boolean slackEnabled, boolean discordEnabled 로 true / false 값만 반환
    @Override
    @Transactional(readOnly = true)
    public NotificationResponse.ChannelsListResponse getChannels(Long userId, Long orgId) {
        validateMember(userId, orgId);

        return settingRepository.findById(orgId)
                .map(NotificationConverter::toChannelsListResponse) // 조직에 연동된 외부채널(디스코드 / 슬랙)이 있으면 DTO 로 반환 -> 해당하는 필드만 true
                .orElseGet(() -> NotificationConverter.emptyChannelsResponse(orgId)); // 없으면 모두 false 로 반환
    }

    // 알림 발송 테스트용
    @Override
    @Transactional(readOnly = true)
    public void sendTest(Long orgId, NotificationRequest.TestSend request) {
        notificationService.sendApiAlarmToOrg(orgId, request.title(), request.message());
    }

    // 암호화 헬퍼 메서드
    private String encryptOrNull(String plain) {
        if (!StringUtils.hasText(plain)) {
            return null;   // null 값이나 빈 값이면 암호화하지 않고 null
        }

        // 웹훅 URL 존재 시 암호화 저장
        try {
            return new String(aesUtil.encryptAES(plain), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CHANNEL_SAVE_FAILED);
        }
    }

    // 검증 헬퍼 메서드
    private void validateAdmin(Long userId, Long orgId) {
        OrgMember member = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new NotificationException(
                        NotificationErrorCode.NOTIFICATION_NOT_ORG_MEMBER));
        if (member.getRole() != OrgRole.ADMIN) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
        }
    }

    private void validateMember(Long userId, Long orgId) {
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new NotificationException(
                        NotificationErrorCode.NOTIFICATION_NOT_ORG_MEMBER));
    }

    private Organization getOrganization(Long orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new NotificationException(
                        NotificationErrorCode.ORG_NOTIFICATION_SETTING_NOT_FOUND));
    }
}
