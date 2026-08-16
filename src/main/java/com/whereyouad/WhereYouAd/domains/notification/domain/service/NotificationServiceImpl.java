package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.exception.NotificationException;
import com.whereyouad.WhereYouAd.domains.notification.exception.code.NotificationErrorCode;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.UserNotification;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgMemberNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.UserNotificationRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.global.utils.cursor.CursorUtil;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.discord.DiscordWebhookClient;
import com.whereyouad.WhereYouAd.infrastructure.client.slack.SlackWebhookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final DiscordWebhookClient discordClient;
    private final SlackWebhookClient slackClient;
    private final AESUtil aesUtil;
    private final UserNotificationRepository userNotificationRepository;

    // 현재 내 알림 설정 조회
    @Override
    public NotificationResponse.MySettings getMySettings(Long userId, Long orgId) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        OrgNotificationSetting orgSetting = orgSettingRepository.findById(orgId).orElse(null);
        return NotificationConverter.toMySettings(setting, orgSetting);
    }

    // 알림 기록 조회 (커서 기반 무한 스크롤) -> 안 읽은 알림 우선, 그룹 내에서는 최신순
    // 읽음 처리 시 정렬 키(isRead)가 바뀌므로 기존 커서는 무효 -> 프론트는 첫 페이지부터 재조회
    @Override
    @Transactional(readOnly = true)
    public NotificationResponse.NotificationHistoryList getHistory(Long userId, Long orgId, String encodedCursor, Integer size) {
        findMember(userId, orgId);

        // 페이지 크기 (기본 20, 상한 50)
        int pageSize = (size != null && size > 0) ? Math.min(size, 50) : 20;

        // 커서엔 id만 담기는데 정렬 키는 (isRead, createdAt, id) 3개 → 커서가 가리키는 행을 조회해 나머지 기준값 확보
        Long cursorId = null;
        Boolean cursorIsRead = null;
        LocalDateTime cursorCreatedAt = null;

        if (encodedCursor != null && !encodedCursor.isBlank()) {
            cursorId = CursorUtil.decodeToId(encodedCursor);
            UserNotification anchor = userNotificationRepository.findCursorAnchor(cursorId, userId, orgId)
                    .orElseThrow(() -> new NotificationException(NotificationErrorCode.INVALID_CURSOR));
            cursorIsRead = anchor.isRead();
            cursorCreatedAt = anchor.getNotification().getCreatedAt();
        }

        // Slice 조회 (pageSize + 1건을 읽어 hasNext를 자동 계산)
        Slice<UserNotification> slice = userNotificationRepository.findHistoryWithCursor(
                userId, orgId, cursorIsRead, cursorCreatedAt, cursorId, PageRequest.of(0, pageSize)
        );

        // 다음 페이지 커서 = 현재 페이지 마지막 행의 id (다음 조회는 이 행 바로 다음부터 시작)
        String nextCursor = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            Long lastId = slice.getContent().get(slice.getContent().size() - 1).getId();
            nextCursor = CursorUtil.encode(lastId);
        }

        return NotificationConverter.toNotificationHistoryList(slice.hasNext(), nextCursor, slice.getContent());
    }

    // 조직 내 회원의 안읽은 알림 단건 읽음 처리
    @Override
    public void markAsRead(Long userId, Long orgId, Long userNotificationId) {
        findMember(userId, orgId);

        UserNotification userNotification = userNotificationRepository.findById(userNotificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOTIFICATION_NOT_FOUND));

        // UserNotification 객체 내부 userId 나 orgId 가 불일치할 경우 모두 Not Found 처리
        if (!Objects.equals(userNotification.getUser().getId(), userId)) {
            throw new NotificationException(NotificationErrorCode.USER_NOTIFICATION_NOT_FOUND);
        }

        if (!Objects.equals(userNotification.getNotification().getOrganization().getId(), orgId)) {
            throw new NotificationException(NotificationErrorCode.USER_NOTIFICATION_NOT_FOUND);
        }

        // Dirty Checking 기반 읽음 처리
        userNotification.markAsRead();
    }

    // 조직 내 회원의 안 읽은 알림 전체 읽음 처리
    @Override
    public void markAllAsRead(Long userId, Long orgId) {
        findMember(userId, orgId);
        userNotificationRepository.markAllAsRead(userId, orgId, LocalDateTime.now());
    }

    // 전체 알림 설정 변경 메서드
    @Override
    public void updateMaster(Long userId, Long orgId, NotificationRequest.UpdateMaster request) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        setting.updateMaster(request.isMasterEnabled());
    }

    // 채널별 알림 설정 메서드 -> 회원 개인 알림설정(이메일, 브라우저 푸시)만
    @Override
    public void updateChannels(Long userId, Long orgId, NotificationRequest.UpdateChannels request) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        setting.updateChannels(request.isBrowserPushEnabled(), request.isEmailEnabled());
    }

    // 알림 기준 설정 메서드(클릭 급증 / 봇 클릭 감지 / 주,일간 보고서)
    @Override
    public void updateAlerts(Long userId, Long orgId, NotificationRequest.UpdateAlerts request) {
        OrgMember member = findMember(userId, orgId);
        OrgMemberNotificationSetting setting = findOrCreateSetting(member);
        setting.updateAlerts(request.alertClicks(), request.alertReport());
    }

    // 조직 단위 알림 트리거 설정 (ADMIN 전용) — 외부 채널로 내보낼 알림 종류 (클릭 급증 / 봇 클릭 감지 / 주,일간 보고서) 설정
    @Override
    public void updateOrgSettings(Long userId, Long orgId, NotificationRequest.UpdateOrgSettings request) {
        OrgMember member = findMember(userId, orgId);
        requireAdmin(member);

        OrgNotificationSetting orgSetting = findOrCreateOrgSetting(member.getOrganization());

        // 활성화와 연결 해제를 동시에 요청하는 모순 방지 (URL 삭제 전에 먼저 차단)
        if ((Boolean.TRUE.equals(request.isSlackEnabled()) && Boolean.TRUE.equals(request.disconnectSlack())) ||
                (Boolean.TRUE.equals(request.isDiscordEnabled()) && Boolean.TRUE.equals(request.disconnectDiscord()))) {
            throw new NotificationException(NotificationErrorCode.CHANNEL_REQUEST_CONFLICT);
        }

        // URL 반영: 삭제 플래그 우선, 아니면 값이 있을 때만 설정 (삭제 시 엔티티가 enabled=false 자동 처리)
        if (Boolean.TRUE.equals(request.disconnectSlack())) {
            orgSetting.updateSlackWebhookUrl(null);
        } else if (StringUtils.hasText(request.slackWebhookUrl())) {
            validateWebhookUrl(request.slackWebhookUrl(), "hooks.slack.com");
            orgSetting.updateSlackWebhookUrl(encryptOrNull(request.slackWebhookUrl()));
        }

        if (Boolean.TRUE.equals(request.disconnectDiscord())) {
            orgSetting.updateDiscordWebhookUrl(null);
        } else if (StringUtils.hasText(request.discordWebhookUrl())) {
            validateWebhookUrl(request.discordWebhookUrl(), "discord.com", "discordapp.com");
            orgSetting.updateDiscordWebhookUrl(encryptOrNull(request.discordWebhookUrl()));
        }

        // URL(요청 또는 기존 DB)이 없는 채널을 활성화하려 하면 거부
        if ((Boolean.TRUE.equals(request.isSlackEnabled()) && !orgSetting.hasSlack()) ||
                (Boolean.TRUE.equals(request.isDiscordEnabled()) && !orgSetting.hasDiscord())) {
            throw new NotificationException(NotificationErrorCode.NO_CHANNEL_URL);
        }

        orgSetting.updateChannelEnabled(request.isSlackEnabled(), request.isDiscordEnabled());

        // 외부 채널로 내보낼 알림 종류 설정
        orgSetting.updateAlerts(request.alertClicks(), request.alertReport());
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

        // 설정이 없는 멤버는 기본값(전부 OFF)에 맞춰 미수신(false)으로 간주
        List<NotificationResponse.MemberSetting> members = slice.getContent().stream()
                .map(m -> {
                    OrgMemberNotificationSetting s = settingsMap.get(m.getId());
                    boolean isReceive = s != null && s.isMasterEnabled();
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

    // 디스코드 / 슬랙 알림 전송 메서드
    // 조직 내에 웹훅 URL 이 설정되어 있는 경우 일괄 전송
    // 추가 : NotificationType 을 입력받아 해당 조직의 수신여부를 확인하고 수신 차단되어 있을경우 알림을 발송하지 않습니다.
    /// *** 각 플랫폼 스케줄러에서 조직으로 디스코드 / 슬랙 알림을 보내려면 이 메서드를 사용하면 됩니다 ***
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendApiAlarmToOrg(Long orgId, NotificationType type, String title, String message) {

        OrgNotificationSetting setting = orgSettingRepository.findById(orgId).orElse(null);

        // 조직 단위로 외부 채널 알림 설정에 이 알림 종류가 꺼져 있으면(또는 설정 없음) 발송 skip
        if (setting == null || !isAlertTypeEnabled(setting, type)) {
            log.debug("[외부 알림 발송 skip] 알림 종류 비활성화/설정없음, type={}, orgId={}", type, orgId);
            return;
        }

        boolean slackActive = setting != null && setting.hasSlack() && setting.isSlackEnabled();
        boolean discordActive = setting != null && setting.hasDiscord() && setting.isDiscordEnabled();

        // 발송할 활성 채널(연결 + 수신 토글 ON)이 하나도 없으면 사유를 채널별로 구분해 로깅 후 skip
        if (!slackActive && !discordActive) {
            logChannelSkip(DeliveryChannel.SLACK, orgId, setting != null && setting.hasSlack());
            logChannelSkip(DeliveryChannel.DISCORD, orgId, setting != null && setting.hasDiscord());
            return;
        }

        // 외부 알림 설정된 조직만 실질적 알림 전송
        sendApiAlarm(setting, orgId, title, message);
    }

    // 알림 발송 테스트용 (설정한 채널이 실제로 동작하는지 확인)
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendTest(Long orgId, NotificationRequest.TestSend request) {
        OrgNotificationSetting setting = orgSettingRepository.findById(orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.ORG_NOTIFICATION_SETTING_NOT_FOUND));

        if (!setting.hasSlack() && !setting.hasDiscord()) {
            throw new NotificationException(NotificationErrorCode.NO_CHANNEL_CONFIGURED);
        }

        sendApiAlarm(setting, orgId, request.title(), request.message());
    }

    // 조직이 해당 알림 종류를 외부 채널(디스코드 / 슬랙)로 수신할 수 있는 상태인지 판별
    // 조건 : 조직 알림 설정 존재 + 알림 종류 ON + (슬랙 or 디스코드 중 하나라도 "URL 등록 + 수신 토글 ON")
    @Override
    @Transactional(readOnly = true)
    public boolean isExternalAlarmActive(Long orgId, NotificationType type) {
        return orgSettingRepository.findById(orgId)
                .filter(setting -> isAlertTypeEnabled(setting, type))
                .filter(setting -> (setting.hasSlack() && setting.isSlackEnabled())
                        || (setting.hasDiscord() && setting.isDiscordEnabled()))
                .isPresent();
    }

    // 실질적 외부 채널 알림 전송 메서드
    private void sendApiAlarm(OrgNotificationSetting setting, Long orgId, String title, String message) {
        if (setting.hasSlack() && setting.isSlackEnabled()) {
            dispatch(DeliveryChannel.SLACK, setting.getSlackWebhookUrl(), orgId,
                    uri -> slackClient.send(uri, NotificationConverter.toSlackMessage(title, message)));
        }

        if (setting.hasDiscord() && setting.isDiscordEnabled()) {
            dispatch(DeliveryChannel.DISCORD, setting.getDiscordWebhookUrl(), orgId,
                    uri -> discordClient.send(uri, NotificationConverter.toDiscordMessage(title, message)));
        }
    }

    // 웹훅 URL이 실제 파싱 가능한 https URL이며 허용 host인지 검증
    private void validateWebhookUrl(String url, String... allowedHosts) {
        final URI uri;
        try {
            uri = new URI(url.trim());   // 형식이 URL이 아니면 URISyntaxException
        } catch (URISyntaxException e) {
            throw new NotificationException(NotificationErrorCode.INVALID_CHANNEL_URL);
        }

        // https + host 존재 확인
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_CHANNEL_URL);
        }

        // 허용된 host 인지 확인 (slack 또는 discord 가 맞는가?)
        boolean hostAllowed = Arrays.stream(allowedHosts)
                .anyMatch(h -> h.equalsIgnoreCase(uri.getHost()));
        if (!hostAllowed) {
            throw new NotificationException(NotificationErrorCode.INVALID_CHANNEL_URL);
        }
    }

    // 알림 종류 입력 받아 실제 조직에서 수신 설정 되어있는지 여부 반환
    private boolean isAlertTypeEnabled(OrgNotificationSetting setting, NotificationType type) {
        return switch (type) {
            case BOT_CLICKS, CLICKS_INCREASE -> setting.isAlertClicks();
            case REPORT -> setting.isAlertReport();
        };
    }

    // 외부 채널 skip 사유 로깅: URL 미설정 vs 설정됐으나 수신 비활성화
    private void logChannelSkip(DeliveryChannel channel, Long orgId, boolean hasUrl) {
        if (hasUrl) {
            log.debug("[외부 알림 발송 skip] {} 웹훅은 설정됐으나 수신 비활성화, orgId={}", channel, orgId);
        } else {
            log.debug("[외부 알림 발송 skip] {} 웹훅 URL 미설정, orgId={}", channel, orgId);
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

    // 웹훅 URL 암호화 (null/빈 값이면 저장하지 않고 null 반환 -> 연동 해제)
    private String encryptOrNull(String plain) {
        if (!StringUtils.hasText(plain)) {
            return null;
        }
        try {
            return new String(aesUtil.encryptAES(plain), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CHANNEL_SAVE_FAILED);
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
