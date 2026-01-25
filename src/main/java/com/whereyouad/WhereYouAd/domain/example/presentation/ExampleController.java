package com.whereyouad.WhereYouAd.domain.example.presentation;

import com.whereyouad.WhereYouAd.domain.example.application.dto.request.ExampleRequest;
import com.whereyouad.WhereYouAd.domain.example.application.dto.response.ExampleResponse;
import com.whereyouad.WhereYouAd.domain.example.domain.service.ExampleService;
import com.whereyouad.WhereYouAd.domain.example.presentation.docs.ExampleControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.response.DefaultIdResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetails;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/examples")
public class ExampleController implements ExampleControllerDocs {

    private final ExampleService exampleService;

    @PostMapping
    public ResponseEntity<DataResponse<DefaultIdResponse>> save(@RequestBody ExampleRequest request) {
        return ResponseEntity.ok(
                DataResponse.created(
                        DefaultIdResponse.of(exampleService.save(request.name()))
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<ExampleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                DataResponse.from(exampleService.findById(id))
        );
    }

    //@AuthenticationPrincipal 예시 메서드
    //회원가입 및 로그인 진행한 뒤,
    //Postman에서 Authorization 탭 -> Bearer Token -> AccessToken 값 붙여넣기
    //or Headers 에서 Authorization 추가하여 Bearer {AccessToken} 붙여넣기
    //해당 회원의 DB에 저장된 Id 값 반환됨.
    @GetMapping("/userId")
    public String userIdTest(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return customUserDetails.getUserId().toString();
    }

    //@AuthenticationPrincipal 예시 메서드
    //CustomUserDetails 내부에 getUserId() 메서드를 통해 회원의 DB 저장된 Id 값 바로 뽑아내기도 가능
    @GetMapping("/userId2")
    public String userIdTest2(@AuthenticationPrincipal(expression = "userId") Long userId) {
        return userId.toString();
    }
}
