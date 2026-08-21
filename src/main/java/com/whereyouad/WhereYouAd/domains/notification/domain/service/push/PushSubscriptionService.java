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

    // 프론트가 pushManager.subscribe() 결과를 서버에 보낼 때 호출. endpoint 유니크 기반 upsert
    public void subscribe(Long userId, Long orgId, NotificationRequest.PushSubscribe request) {
        OrgMember member = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime expiration = request.expirationTime() == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(request.expirationTime()), ZoneId.systemDefault());

        subscriptionRepository.findByEndpoint(request.endpoint())
                .ifPresentOrElse(
                        existing -> {
                            // 다른 멤버가 같은 endpoint 로 재구독 -> 삭제 후 재생성 (기기 소유권 이전)
                            subscriptionRepository.delete(existing);
                            subscriptionRepository.flush();
                            subscriptionRepository.save(NotificationConverter.toPushSubscription(member, request, expiration));
                        },
                        () -> subscriptionRepository.save(NotificationConverter.toPushSubscription(member, request, expiration))
                );
    }

    public void unsubscribe(Long userId, Long orgId, String endpoint) {
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.MEMBER_NOT_FOUND));
        subscriptionRepository.deleteByEndpoint(endpoint);
    }
}
