package com.whereyouad.WhereYouAd.domains.notification.domain.service.push;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.exception.NotificationException;
import com.whereyouad.WhereYouAd.domains.notification.exception.code.NotificationErrorCode;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.PushSubscriptionRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository subscriptionRepository;
    private final OrgMemberRepository orgMemberRepository;

    @Value("${web-push.vapid.public-key}")
    private String vapidPublicKey;

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    // 프론트가 pushManager.subscribe() 결과를 서버에 보낼 때 호출. 멤버십+endpoint 기반 upsert
    public void subscribe(Long userId, Long orgId, NotificationRequest.PushSubscribe request) {
        OrgMember member = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime expiration = request.expirationTime() == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(request.expirationTime()), ZoneId.systemDefault());
        var validatedSubscription = NotificationConverter.toPushSubscription(member, request, expiration);

        subscriptionRepository.findByOrgMember_IdAndEndpoint(member.getId(), request.endpoint())
                .ifPresentOrElse(
                        existing -> existing.update(
                                validatedSubscription.getP256dhKey(),
                                validatedSubscription.getAuthSecret(),
                                validatedSubscription.getUserAgent(),
                                validatedSubscription.getExpirationTime()),
                        () -> subscriptionRepository.save(validatedSubscription)
                );
    }

    public void unsubscribe(Long userId, Long orgId, String endpoint) {
        OrgMember member = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.MEMBER_NOT_FOUND));
        subscriptionRepository.deleteByOrgMember_IdAndEndpoint(member.getId(), endpoint);
    }
}
