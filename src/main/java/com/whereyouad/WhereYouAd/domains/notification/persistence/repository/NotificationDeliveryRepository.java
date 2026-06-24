package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;


public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

}
