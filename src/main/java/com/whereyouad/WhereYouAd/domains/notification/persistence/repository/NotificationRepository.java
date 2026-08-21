package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 보관 기간이 지난 알림 id 조회 (청크 단위로 끊어서 조회)
    @Query("SELECT n.id FROM Notification n WHERE n.createdAt < :threshold ORDER BY n.createdAt, n.id")
    List<Long> findIdsByCreatedAtBefore(@Param("threshold") LocalDateTime threshold, Pageable pageable);
}
