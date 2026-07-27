package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
