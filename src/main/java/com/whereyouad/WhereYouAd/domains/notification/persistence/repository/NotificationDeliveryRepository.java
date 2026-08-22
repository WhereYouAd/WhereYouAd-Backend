package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryStatus;
import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationDelivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    // 특정 알림의 채널별 미완료(PENDING/FAILED) 발송 기록. 재시도 시에도 재사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT nd FROM NotificationDelivery nd " +
            "JOIN FETCH nd.orgMember om " +
            "JOIN FETCH nd.notification n " +
            "WHERE nd.notification.id = :notificationId " +
            "AND nd.channel = :channel " +
            "AND nd.status IN (com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryStatus.PENDING," +
            "                  com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryStatus.FAILED)")
    List<NotificationDelivery> findPendingOrFailedByNotificationAndChannel(
            @Param("notificationId") Long notificationId,
            @Param("channel") DeliveryChannel channel);

    // 재시도 스케줄러가 대상으로 삼을 실패 delivery. notification 을 join fetch 해 Kafka 이벤트 재구성에 사용
    @Query("SELECT nd FROM NotificationDelivery nd " +
            "JOIN FETCH nd.notification n " +
            "JOIN FETCH n.organization o " +
            "WHERE nd.channel = :channel " +
            "AND nd.status = :status " +
            "AND nd.retryCount < :maxRetryCount " +
            "ORDER BY nd.id")
    List<NotificationDelivery> findRetryTargets(
            @Param("channel") DeliveryChannel channel,
            @Param("status") DeliveryStatus status,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT nd FROM NotificationDelivery nd " +
            "WHERE nd.channel = :channel " +
            "AND nd.status = :status " +
            "AND nd.processingStartedAt < :cutoff")
    List<NotificationDelivery> findStaleProcessing(
            @Param("channel") DeliveryChannel channel,
            @Param("status") DeliveryStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);
}
