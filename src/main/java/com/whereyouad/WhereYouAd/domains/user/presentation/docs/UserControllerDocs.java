package com.whereyouad.WhereYouAd.domains.user.presentation.docs;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.EmailRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.EmailVerifyRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.EmailSentResponse;
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
            description = "이메일, 비밀번호, 이름, 전화번호를 받아 회원가입을 진행합니다(먼저 이메일 인증이 진행되어야 회원가입 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_2", description = "이메일 중복 회원 존재")
    })
    public ResponseEntity<DataResponse<SignUpResponse>> signUp(@RequestBody @Valid SignUpRequest request);

    @Operation(
            summary = "이메일 인증코드 전송 API",
            description = "입력받은 이메일로 인증코드를 전송합니다. 인증코드 재전송도 해당 API 를 호출합니다.\n\n" +
                    "테스트용 이메일은 'test' 로 시작하거나 'example.com' 으로 끝나야합니다. 테스트용 이메일의 인증코드는 서버 로그로 확인 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_3", description = "이메일 전송실패(이메일 오타 등)")
    })
    public ResponseEntity<DataResponse<EmailSentResponse>> sendEmail(@RequestBody @Valid EmailRequest request);

    @Operation(
            summary = "이메일 인증코드 인증 API",
            description = "이메일과 인증코드를 받아 인증코드가 맞는지 검증합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_4", description = "실패(인증코드 불일치)")
    })
    public ResponseEntity<DataResponse<String>> verifyEmail(@RequestBody @Valid EmailVerifyRequest request);
}
