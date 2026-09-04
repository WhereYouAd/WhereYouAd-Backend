package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.NotificationAlertEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationInboxClaimResult;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationAlertInboxService;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationAlertInboxService inboxService;

    @InjectMocks
    private NotificationConsumer consumer;

    @Test
    void completesInboxOnlyAfterBothDispatchesSucceed() {
        NotificationAlertEvent event = event("event-1");
        when(inboxService.claim("event-1")).thenReturn(NotificationInboxClaimResult.CLAIMED);

        consumer.consume(event);

        verify(notificationService).sendApiAlarmToOrgOrThrow(
                1L, NotificationType.REPORT, "title", "message");
        verify(notificationService).sendBrowserPushToOrgOrThrow(
                1L, NotificationType.REPORT, "title", "message", null);
        verify(inboxService).complete("event-1");
        verify(inboxService, never()).fail("event-1");
    }

    @Test
    void marksInboxFailedAndPropagatesDispatchFailure() {
        NotificationAlertEvent event = event("event-2");
        when(inboxService.claim("event-2")).thenReturn(NotificationInboxClaimResult.CLAIMED);
        doThrow(new IllegalStateException("dispatch failed"))
                .when(notificationService)
                .sendApiAlarmToOrgOrThrow(1L, NotificationType.REPORT, "title", "message");

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("dispatch failed");

        verify(inboxService).fail("event-2");
        verify(inboxService, never()).complete("event-2");
        verify(notificationService, never()).sendBrowserPushToOrgOrThrow(
                1L, NotificationType.REPORT, "title", "message", null);
    }

    @Test
    void skipsCompletedOrActivelyProcessingDuplicate() {
        NotificationAlertEvent event = event("event-3");
        when(inboxService.claim("event-3")).thenReturn(NotificationInboxClaimResult.COMPLETED);

        consumer.consume(event);

        verify(notificationService, never()).sendApiAlarmToOrgOrThrow(
                1L, NotificationType.REPORT, "title", "message");
        verify(inboxService, never()).complete("event-3");
        verify(inboxService, never()).fail("event-3");
    }

    @Test
    void propagatesActivelyProcessingEventSoKafkaDoesNotCommitIt() {
        NotificationAlertEvent event = event("event-4");
        when(inboxService.claim("event-4")).thenReturn(NotificationInboxClaimResult.PROCESSING);

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("처리 중");

        verify(notificationService, never()).sendApiAlarmToOrgOrThrow(
                1L, NotificationType.REPORT, "title", "message");
        verify(inboxService, never()).complete("event-4");
        verify(inboxService, never()).fail("event-4");
    }

    private NotificationAlertEvent event(String eventId) {
        return NotificationAlertEvent.builder()
                .eventId(eventId)
                .orgId(1L)
                .type(NotificationType.REPORT)
                .title("title")
                .message("message")
                .build();
    }
}
