package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationInboxClaimResult;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationInboxStatus;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationAlertInboxRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationAlertInboxServiceTest {

    private final NotificationAlertInboxRepository repository = mock(NotificationAlertInboxRepository.class);
    private final NotificationAlertInboxService service = new NotificationAlertInboxService(repository);

    @Test
    void claimsNewEvent() {
        when(repository.insertProcessingIfAbsent("event-1")).thenReturn(1);

        assertThat(service.claim("event-1")).isEqualTo(NotificationInboxClaimResult.CLAIMED);
    }

    @Test
    void reclaimsFailedOrStaleProcessingEvent() {
        when(repository.insertProcessingIfAbsent("event-2")).thenReturn(0);
        when(repository.reclaimFailedOrStale(eq("event-2"), any(LocalDateTime.class))).thenReturn(1);

        assertThat(service.claim("event-2")).isEqualTo(NotificationInboxClaimResult.CLAIMED);
    }

    @Test
    void distinguishesCompletedFromActivelyProcessingEvent() {
        when(repository.insertProcessingIfAbsent("completed")).thenReturn(0);
        when(repository.findStatusByEventId("completed")).thenReturn(Optional.of(NotificationInboxStatus.COMPLETED));
        when(repository.insertProcessingIfAbsent("processing")).thenReturn(0);
        when(repository.findStatusByEventId("processing")).thenReturn(Optional.of(NotificationInboxStatus.PROCESSING));

        assertThat(service.claim("completed")).isEqualTo(NotificationInboxClaimResult.COMPLETED);
        assertThat(service.claim("processing")).isEqualTo(NotificationInboxClaimResult.PROCESSING);
    }

    @Test
    void transitionsClaimToTerminalState() {
        service.complete("event-3");
        service.fail("event-4");

        verify(repository).markCompleted("event-3");
        verify(repository).markFailed("event-4");
    }
}
