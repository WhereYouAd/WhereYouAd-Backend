package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationAlertInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationAlertInboxService {

    private final NotificationAlertInboxRepository inboxRepository;

    @Transactional
    public boolean claim(String eventId) {
        return inboxRepository.insertIfAbsent(eventId) == 1;
    }
}
