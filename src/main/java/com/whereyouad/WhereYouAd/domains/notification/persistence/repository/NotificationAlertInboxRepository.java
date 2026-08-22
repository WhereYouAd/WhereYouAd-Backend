package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationAlertInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationAlertInboxRepository extends JpaRepository<NotificationAlertInbox, String> {

    @Modifying
    @Query(value = "INSERT IGNORE INTO notification_alert_inbox (event_id, processed_at) " +
            "VALUES (:eventId, CURRENT_TIMESTAMP)", nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String eventId);
}
