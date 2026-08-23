package com.whereyouad.WhereYouAd.domains.notification.domain.service.push;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryStatus;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.Notification;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationDelivery;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationDeliveryRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.OrgMemberNotificationSettingRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.PushSubscriptionRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.UserNotificationRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class BrowserPushRetryIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Test
    void retryEventCarriesOnlyDeliveryIdsBelowRetryLimit() {
        User user = entityManager.merge(User.builder()
                .email("retry@example.com")
                .name("retry-user")
                .status(UserStatus.ACTIVE)
                .build());
        Organization organization = entityManager.merge(Organization.builder()
                .name("retry-org")
                .ownerUserId(user.getId())
                .status(OrgStatus.ACTIVE)
                .build());
        OrgMember member = entityManager.merge(OrgMember.builder()
                .role(OrgRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .user(user)
                .organization(organization)
                .build());
        Notification notification = notificationRepository.save(Notification.builder()
                .title("retry-title")
                .message("retry-message")
                .type(NotificationType.REPORT)
                .organization(organization)
                .build());
        NotificationDelivery eligible = deliveryRepository.save(delivery(notification, member, 2));
        NotificationDelivery exhausted = deliveryRepository.save(delivery(notification, member, 3));
        entityManager.flush();

        PushSubscriptionRepository subscriptionRepository = mock(PushSubscriptionRepository.class);
        when(subscriptionRepository.findAllByMembershipIds(any())).thenReturn(List.of());
        BrowserPushDataAccess dataAccess = new BrowserPushDataAccess(
                notificationRepository,
                mock(UserNotificationRepository.class),
                deliveryRepository,
                mock(OrgMemberNotificationSettingRepository.class),
                subscriptionRepository,
                mock(OrgRepository.class));

        List<PushNotificationEvent> events = dataAccess.loadRetryEvents(3, 20);

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getNotificationId()).isEqualTo(notification.getId());
            assertThat(event.getDeliveryIds()).containsExactly(eligible.getId());
        });

        dataAccess.loadTargets(events.get(0).getNotificationId(), events.get(0).getDeliveryIds());
        entityManager.flush();
        entityManager.clear();

        assertThat(deliveryRepository.findById(eligible.getId()).orElseThrow().getStatus())
                .isEqualTo(DeliveryStatus.PROCESSING);
        assertThat(deliveryRepository.findById(exhausted.getId()).orElseThrow().getStatus())
                .isEqualTo(DeliveryStatus.FAILED);
    }

    private NotificationDelivery delivery(Notification notification, OrgMember member, int retryCount) {
        return NotificationDelivery.builder()
                .notification(notification)
                .orgMember(member)
                .channel(DeliveryChannel.BROWSER_PUSH)
                .status(DeliveryStatus.FAILED)
                .retryCount(retryCount)
                .build();
    }
}
