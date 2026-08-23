package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationInboxClaimResult;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationInboxStatus;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationAlertInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationAlertInboxService {

    private static final long PROCESSING_TIMEOUT_MINUTES = 10;

    private final NotificationAlertInboxRepository inboxRepository;

    @Transactional
    public NotificationInboxClaimResult claim(String eventId) {
        if (inboxRepository.insertProcessingIfAbsent(eventId) == 1) {
            return NotificationInboxClaimResult.CLAIMED;
        }
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        if (inboxRepository.reclaimFailedOrStale(eventId, staleBefore) == 1) {
            return NotificationInboxClaimResult.CLAIMED;
        }
        return inboxRepository.findStatusByEventId(eventId)
                .filter(status -> status == NotificationInboxStatus.COMPLETED)
                .map(status -> NotificationInboxClaimResult.COMPLETED)
                .orElse(NotificationInboxClaimResult.PROCESSING);
    }

    @Transactional
    public void complete(String eventId) {
        inboxRepository.markCompleted(eventId);
    }

    @Transactional
    public void fail(String eventId) {
        inboxRepository.markFailed(eventId);
    }
}
