package com.whereyouad.WhereYouAd.domains.notification.presentation;

import com.whereyouad.WhereYouAd.domains.notification.presentation.docs.NotificationControllerDocs;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {
}
