package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationAlertInbox;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationInboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationAlertInboxRepository extends JpaRepository<NotificationAlertInbox, String> {

    @Modifying
    @Query(value = "INSERT IGNORE INTO notification_alert_inbox (event_id, status, processed_at) " +
            "VALUES (:eventId, 'PROCESSING', CURRENT_TIMESTAMP)", nativeQuery = true)
    int insertProcessingIfAbsent(@Param("eventId") String eventId);

    @Modifying
    @Query(value = "UPDATE notification_alert_inbox " +
            "SET status = 'PROCESSING', processed_at = CURRENT_TIMESTAMP " +
            "WHERE event_id = :eventId " +
            "AND (status = 'FAILED' OR (status = 'PROCESSING' AND processed_at < :staleBefore))",
            nativeQuery = true)
    int reclaimFailedOrStale(
            @Param("eventId") String eventId,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Modifying
    @Query(value = "UPDATE notification_alert_inbox " +
            "SET status = 'COMPLETED', processed_at = CURRENT_TIMESTAMP " +
            "WHERE event_id = :eventId AND status = 'PROCESSING'",
            nativeQuery = true)
    int markCompleted(@Param("eventId") String eventId);

    @Modifying
    @Query(value = "UPDATE notification_alert_inbox " +
            "SET status = 'FAILED', processed_at = CURRENT_TIMESTAMP " +
            "WHERE event_id = :eventId AND status = 'PROCESSING'",
            nativeQuery = true)
    int markFailed(@Param("eventId") String eventId);

    @Query("SELECT inbox.status FROM NotificationAlertInbox inbox WHERE inbox.eventId = :eventId")
    Optional<NotificationInboxStatus> findStatusByEventId(@Param("eventId") String eventId);
}
