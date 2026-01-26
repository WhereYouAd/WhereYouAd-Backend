package com.whereyouad.WhereYouAd.domain.example.presentation.docs;

import com.whereyouad.WhereYouAd.domain.example.application.dto.request.ExampleRequest;
import com.whereyouad.WhereYouAd.domain.example.application.dto.response.ExampleResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.response.DefaultIdResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ExampleControllerDocs {
    @Operation(
            summary = "예시 이름 저장 API",
            description = "이름을 받아와서 DB에 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "실패")
    })
    public ResponseEntity<DataResponse<DefaultIdResponse>> save(@RequestBody ExampleRequest request);

    @Operation(
            summary = "예시 이름 조회 API",
            description = "id값에 해당하는 이름을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "실패")
    })
    public ResponseEntity<DataResponse<ExampleResponse>> findById(@PathVariable Long id);

    @Operation(
            summary = "@AuthenticationPrincipal 사용법 예시 API",
            description = "회원가입 -> 로그인 후 AccessToken 을 가지고 해당 메서드 호출 시 DB 내부 회원의 Id 반환"
    )
    @ApiResponses(
            @ApiResponse(responseCode = "200", description = "성공")
    )
    public String userIdTest(@AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(
            summary = "@AuthenticationPrincipal 사용법 예시 API",
            description = "회원가입 -> 로그인 후 AccessToken 을 가지고 해당 메서드 호출 시 DB 내부 회원의 Id 반환, CustomUserDetails 내부 편의 메서드 사용 예시"
    )
    @ApiResponses(
            @ApiResponse(responseCode = "200", description = "성공")
    )
    public String userIdTest2(@AuthenticationPrincipal(expression = "userId") Long userId);
}
