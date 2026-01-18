package com.whereyouad.WhereYouAd.domains.user.presentation;

import com.whereyouad.WhereYouAd.domains.user.domain.service.UserService;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.presentation.docs.UserControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<DataResponse<SignUpResponse>> signUp(@RequestBody @Valid SignUpRequest request)
    {
        SignUpResponse signUpResponse = userService.signUpUser(request);
        return ResponseEntity.ok(
                DataResponse.created(signUpResponse)
        );
    }

}
