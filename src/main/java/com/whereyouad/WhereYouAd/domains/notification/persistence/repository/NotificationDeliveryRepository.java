package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    // 만료 알림 정리용 - 발송 이력 벌크 삭제
    @Modifying
    @Query("DELETE FROM NotificationDelivery nd WHERE nd.notification.id IN :notificationIds")
    int deleteByNotificationIdIn(@Param("notificationIds") List<Long> notificationIds);
}
