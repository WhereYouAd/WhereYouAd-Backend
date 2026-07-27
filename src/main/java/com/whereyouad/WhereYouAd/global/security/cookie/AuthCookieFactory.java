package com.whereyouad.WhereYouAd.global.security.cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthCookieFactory {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final boolean secure;
    private final String domain;
    private final String sameSite;

    public AuthCookieFactory(
            @Value("${cookie.secure}") boolean secure,
            @Value("${cookie.domain:}") String domain,
            @Value("${cookie.same-site}") String sameSite
    ) {
        this.secure = secure;
        this.domain = domain;
        this.sameSite = sameSite;
    }

    public ResponseCookie createAccessTokenCookie(String value, long maxAge) {
        return create(ACCESS_TOKEN_COOKIE, value, false, maxAge);
    }

    public ResponseCookie createRefreshTokenCookie(String value, long maxAge) {
        return create(REFRESH_TOKEN_COOKIE, value, true, maxAge);
    }

    private ResponseCookie create(String name, String value, boolean httpOnly, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(maxAge);
        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }
        return builder.build();
    }
}
