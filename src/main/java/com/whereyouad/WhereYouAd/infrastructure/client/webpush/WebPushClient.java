package com.whereyouad.WhereYouAd.infrastructure.client.webpush;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Security;

@Slf4j
@Component
public class WebPushClient {

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private PushService pushService;

    public WebPushClient(
            @Value("${web-push.vapid.public-key}") String publicKey,
            @Value("${web-push.vapid.private-key}") String privateKey,
            @Value("${web-push.vapid.subject}") String subject
    ) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    @PostConstruct
    void init() {
        // BouncyCastle 는 VAPID 서명(ES256) 에 사용되며 JDK 기본 provider 로는 web-push-java 가 동작하지 않음
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            this.pushService = new PushService()
                    .setPublicKey(publicKey)
                    .setPrivateKey(privateKey)
                    .setSubject(subject);
        } catch (Exception e) {
            throw new IllegalStateException("VAPID 키 초기화 실패 - 환경변수(VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY) 값 확인 필요", e);
        }
    }

    // 성공/실패 여부와 무관하게 HTTP 상태 코드를 그대로 반환. 호출자가 200~299/404/410 을 판별해 delivery 상태 결정
    public int send(String endpoint, String p256dh, String auth, String payload) {
        try {
            Notification notification = new Notification(
                    endpoint,
                    p256dh,
                    auth,
                    payload.getBytes(StandardCharsets.UTF_8)
            );
            HttpResponse response = pushService.send(notification);
            return response.getStatusLine().getStatusCode();
        } catch (Exception e) {
            throw new WebPushSendException("Web Push 발송 오류: " + e.getMessage(), 0, e);
        }
    }
}
