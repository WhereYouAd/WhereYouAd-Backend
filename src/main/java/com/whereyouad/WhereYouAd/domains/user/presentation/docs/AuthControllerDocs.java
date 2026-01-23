package com.whereyouad.WhereYouAd.domains.user.presentation.docs;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.LoginRequest;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthControllerDocs {
    @Operation(
            summary = "로그인 API",
            description = "이메일, 비밀번호를 입력받아 로그인 진행, AccessToken 을 body 로 반환 & RefreshToken 은 쿠키로 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_1", description = "실패")
    })
    public ResponseEntity<DataResponse<TokenResponse>> login(@RequestBody LoginRequest request);

    @Operation(
            summary = "AccessToken 재발급 API",
            description = "AccessToken 만료 시 쿠키에 있는 RefreshToken 을 사용해 AccessToken & RefreshToken 을 재발급"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "실패(RefreshToken 만료, 재로그인 필요)"),
            @ApiResponse(responseCode = "401", description = "실패(RefreshToken 옳지 않은 값)")
    })
    public ResponseEntity<DataResponse<TokenResponse>> reissue(@CookieValue(name = "refresh_token") String refreshToken);
}
