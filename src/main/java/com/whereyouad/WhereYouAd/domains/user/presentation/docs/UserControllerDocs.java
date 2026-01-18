package com.whereyouad.WhereYouAd.domains.user.presentation.docs;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserControllerDocs {
    @Operation(
            summary = "단순 회원가입 API",
            description = "이메일, 비밀번호, 이름, 전화번호를 받아 회원가입을 진행합니다(이메일 인증 미진행상태)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_2", description = "이메일 중복 회원 존재")
    })
    public ResponseEntity<DataResponse<SignUpResponse>> signUp(@RequestBody @Valid SignUpRequest request);
}
