package com.whereyouad.WhereYouAd.global.adapi;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;

import java.security.GeneralSecurityException;
import java.util.Map;

public interface AdAuthStrategy {

    // 지원하는 광고 플랫폼 반환
    Provider getProvider();

    // DB에서 조회된 인증 정보(connection)와 추가 request를 기반으로 헤더 Map을 생성
    Map<String, String> generateHeaders(PlatformConnection connection, AdAuthRequest request) throws GeneralSecurityException;
}
