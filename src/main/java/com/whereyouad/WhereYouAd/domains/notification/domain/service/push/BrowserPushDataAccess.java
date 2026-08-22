package com.whereyouad.WhereYouAd.domains.notification.domain.service.push;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushDeliveryResult;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushDeliveryTarget;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;
import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryStatus;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.Notification;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationDelivery;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.PushSubscription;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.UserNotification;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationDeliveryRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgMemberNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.PushSubscriptionRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.UserNotificationRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 브라우저 푸시 발송 파이프라인의 DB 접근 계층.
// 오케스트레이션(BrowserPushDispatcher) 은 이 클래스의 짧은 트랜잭션 메서드만 호출하고,
// 외부 HTTP(Web Push) 호출은 트랜잭션 밖에서 수행 - DB 커넥션 점유 최소화
@Slf4j
@Service
@RequiredArgsConstructor
public class BrowserPushDataAccess {

    private static final long PROCESSING_TIMEOUT_MINUTES = 10;

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final OrgMemberNotificationSettingRepository memberSettingRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final OrgRepository orgRepository;

    @Transactional(readOnly = true)
    public boolean hasPushTargets(Long orgId, NotificationType type) {
        return !memberSettingRepository.findPushEnabledForOrg(
                orgId, isClickType(type), isReportType(type)).isEmpty();
    }

    // 발송 최초 트리거: Notification + 각 대상 멤버의 UserNotification/NotificationDelivery(PENDING) 를 원자적으로 저장.
    // 대상 멤버가 없으면 null 반환 (호출자는 Kafka 발행 skip)
    @Transactional
    public Long persistPushNotification(Long orgId, NotificationType type, String title, String body, String linkUrl) {
        List<OrgMemberNotificationSetting> targets = memberSettingRepository.findPushEnabledForOrg(
                orgId, isClickType(type), isReportType(type));
        if (targets.isEmpty()) {
            return null;
        }

        Organization organization = orgRepository.findById(orgId).orElse(null);
        if (organization == null) {
            log.warn("[웹푸시] 발송 대상 조직 없음 orgId={}", orgId);
            return null;
        }

        Notification notification = notificationRepository.save(
                NotificationConverter.toNotification(organization, type, title, body, linkUrl));

        List<UserNotification> userNotifications = new ArrayList<>(targets.size());
        List<NotificationDelivery> deliveries = new ArrayList<>(targets.size());
        for (OrgMemberNotificationSetting s : targets) {
            userNotifications.add(NotificationConverter.toUserNotification(notification, s.getOrgMember()));
            deliveries.add(NotificationConverter.toPendingDelivery(notification, s.getOrgMember(), DeliveryChannel.BROWSER_PUSH));
        }
        userNotificationRepository.saveAll(userNotifications);
        deliveryRepository.saveAll(deliveries);
        return notification.getId();
    }

    // Consumer 가 발송 직전 대상 로드. delivery + 각 멤버의 subscription 을 카티지언 없이 2단계 조회
    @Transactional
    public List<PushDeliveryTarget> loadTargets(Long notificationId) {
        List<NotificationDelivery> deliveries = deliveryRepository
                .findPendingOrFailedByNotificationAndChannel(notificationId, DeliveryChannel.BROWSER_PUSH);
        if (deliveries.isEmpty()) {
            return List.of();
        }
        deliveries.forEach(NotificationDelivery::markProcessing);

        List<Long> membershipIds = deliveries.stream()
                .map(d -> d.getOrgMember().getId())
                .distinct()
                .toList();

        Map<Long, List<PushSubscription>> subsByMember = subscriptionRepository
                .findAllByMembershipIds(membershipIds).stream()
                .collect(Collectors.groupingBy(s -> s.getOrgMember().getId()));

        List<PushDeliveryTarget> targets = new ArrayList<>();
        for (NotificationDelivery delivery : deliveries) {
            List<PushSubscription> subs = subsByMember.getOrDefault(delivery.getOrgMember().getId(), List.of());
            if (subs.isEmpty()) {
                // 구독이 하나도 없는 멤버는 즉시 실패 처리 (재시도해도 계속 실패)
                targets.add(NotificationConverter.toEmptyPushDeliveryTarget(delivery));
                continue;
            }
            for (PushSubscription s : subs) {
                targets.add(NotificationConverter.toPushDeliveryTarget(delivery, s));
            }
        }
        return targets;
    }

    // 발송 결과 반영. 한 delivery 에 대한 여러 subscription 결과를 취합 - 하나라도 SUCCESS 면 SUCCESS
    // 재시도 상한은 스케줄러 쪽에서만 필터링에 사용되므로 여기서는 상한 검사를 하지 않는다.
    @Transactional
    public void recordResults(List<PushDeliveryResult> results) {
        Map<Long, List<PushDeliveryResult>> resultsByDelivery = results.stream()
                .collect(Collectors.groupingBy(PushDeliveryResult::deliveryId));
        Map<Long, NotificationDelivery> deliveriesById = deliveryRepository
                .findAllById(resultsByDelivery.keySet()).stream()
                .collect(Collectors.toMap(NotificationDelivery::getId, delivery -> delivery));
        Set<Long> expiredSubscriptionIds = new HashSet<>();

        for (Map.Entry<Long, List<PushDeliveryResult>> entry : resultsByDelivery.entrySet()) {
            NotificationDelivery delivery = deliveriesById.get(entry.getKey());
            if (delivery == null || delivery.getStatus() != DeliveryStatus.PROCESSING) continue;

            List<PushDeliveryResult> perDelivery = entry.getValue();
            boolean anySuccess = perDelivery.stream().anyMatch(PushDeliveryResult::isSuccess);
            if (anySuccess) {
                delivery.markSuccess();
            } else {
                String reason = perDelivery.stream()
                        .map(PushDeliveryResult::failReasonSummary)
                        .filter(r -> r != null && !r.isBlank())
                        .findFirst()
                        .orElse("no successful delivery");
                delivery.markFailed(truncate(reason));
            }

            // 만료(410/404) 로 판정된 구독은 재시도해도 계속 실패하므로 즉시 삭제
            perDelivery.stream()
                    .filter(PushDeliveryResult::isExpired)
                    .map(PushDeliveryResult::subscriptionId)
                    .filter(java.util.Objects::nonNull)
                    .forEach(expiredSubscriptionIds::add);
        }

        if (!expiredSubscriptionIds.isEmpty()) {
            try {
                subscriptionRepository.deleteAllByIdInBatch(expiredSubscriptionIds);
            } catch (Exception e) {
                log.warn("[웹푸시] 만료 구독 일괄 삭제 실패 subscriptionIds={}", expiredSubscriptionIds, e);
            }
        }
    }

    @Transactional
    public void markPublicationFailed(Long notificationId, String reason) {
        String failureReason = reason == null || reason.isBlank() ? "Kafka publication failed" : reason;
        deliveryRepository.findPendingOrFailedByNotificationAndChannel(
                        notificationId, DeliveryChannel.BROWSER_PUSH).stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.PENDING)
                .forEach(delivery -> delivery.markFailed(truncate(failureReason)));
    }

    // 재시도 스케줄러가 대상 delivery 를 조회해 Kafka 재발행용 이벤트로 변환
    @Transactional
    public List<PushNotificationEvent> loadRetryEvents(int maxRetryCount, int batchSize) {
        LocalDateTime staleCutoff = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        deliveryRepository.findStaleProcessing(
                        DeliveryChannel.BROWSER_PUSH,
                        DeliveryStatus.PROCESSING,
                        staleCutoff,
                        PageRequest.of(0, batchSize))
                .forEach(delivery -> delivery.markFailed("processing claim timed out"));

        return deliveryRepository.findRetryTargets(
                        DeliveryChannel.BROWSER_PUSH,
                        DeliveryStatus.FAILED,
                        maxRetryCount,
                        PageRequest.of(0, batchSize))
                .stream()
                .map(NotificationDelivery::getNotification)
                .distinct()
                .map(NotificationConverter::toPushNotificationEvent)
                .toList();
    }

    private boolean isClickType(NotificationType type) {
        return type == NotificationType.BOT_CLICKS || type == NotificationType.CLICKS_INCREASE;
    }

    private boolean isReportType(NotificationType type) {
        return type == NotificationType.REPORT;
    }

    private String truncate(String s) {
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
