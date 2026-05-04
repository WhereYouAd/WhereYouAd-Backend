package com.whereyouad.WhereYouAd.global.adapi.strategy;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.ErrorCode;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.global.adapi.AdAuthStrategy;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverAdAuthStrategy implements AdAuthStrategy {

    private final AESUtil aesUtil;

    @Override
    public Provider getProvider() {
        return Provider.NAVER;
    }

    @Override
    public Map<String, String> generateHeaders(PlatformConnection connection, AdAuthRequest request) throws GeneralSecurityException {
        if (request.method() == null || request.path() == null) {
            log.error("Naver 요청에는 method와 path가 필수 입니다.");
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Map<String, String> headers = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis());

        // DB에서 암호화된 키 복호화
        String apiKey = connection.getAuthIdentifier();
        String secretKey = new String(aesUtil.decryptAES(connection.getAuthCredential()), StandardCharsets.UTF_8);
        String customerId = connection.getPlatformAccount().getExternalAccountId();

        // HMAC-SHA256 서명 생성
        String message = timestamp + "." + request.method() + "." + request.path();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));

        // 헤더 세팅
        headers.put("X-Timestamp", timestamp);
        headers.put("X-API-KEY", apiKey);
        headers.put("X-Customer", customerId);
        headers.put("X-Signature", signature);

        return headers;
    }
}
