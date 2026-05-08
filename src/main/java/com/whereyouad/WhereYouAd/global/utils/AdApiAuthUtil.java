package com.whereyouad.WhereYouAd.global.utils;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.global.adapi.AdAuthFactory;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdApiAuthUtil {

    private final PlatformConnectionRepository platformConnectionRepository;
    private final AdAuthFactory adAuthFactory;

    /**
     * 외부 API 통신을 위한 HTTP 인증 헤더 Map을 생성합니다.
     * @param connectionId 연결 정보
     * @param request 파라미터 (method, path 등)
     * @return HTTP Header에 삽입할 Key-Value Map
     */
    @Transactional(readOnly = true)
    public Map<String, String> generateAuthHeaders(
            @NotNull Long connectionId,
            @NotNull AdAuthRequest request
    ) throws GeneralSecurityException {

        PlatformConnection connection = platformConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));

        Provider provider = connection.getPlatformAccount().getProvider();
        log.debug("[{}] API 인증 헤더 생성 시작 - connectionId: {}", provider, connectionId);

        return adAuthFactory.getStrategy(provider).generateHeaders(connection, request);
    }
}