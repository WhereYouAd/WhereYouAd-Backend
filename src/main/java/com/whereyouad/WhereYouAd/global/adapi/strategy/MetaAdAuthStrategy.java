package com.whereyouad.WhereYouAd.global.adapi.strategy;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.global.adapi.AdAuthStrategy;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MetaAdAuthStrategy implements AdAuthStrategy {

    private final AESUtil aesUtil;

    @Override
    public Provider getProvider() {
        return Provider.META;
    }

    @Override
    public Map<String, String> generateHeaders(PlatformConnection connection, AdAuthRequest request) throws GeneralSecurityException {
        Map<String, String> headers = new HashMap<>();

        // 1. 암호화된 액세스 토큰 복호화
        String accessToken = new String(aesUtil.decryptAES(connection.getAuthIdentifier()), StandardCharsets.UTF_8).trim();

        // 헤더 생성 로직 추가
        // Authorization 헤더 (Bearer 토큰)
        // 헤더 대신 파라미터 이름표로 맵핑(FeignClient에서 꺼내 쓸 용도)
        headers.put("access_token", accessToken);

        return headers;
    }
}
