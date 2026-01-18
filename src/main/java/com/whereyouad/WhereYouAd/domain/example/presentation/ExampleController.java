package com.whereyouad.WhereYouAd.domain.example.presentation;

import com.whereyouad.WhereYouAd.domain.example.application.dto.request.ExampleRequest;
import com.whereyouad.WhereYouAd.domain.example.application.dto.response.ExampleResponse;
import com.whereyouad.WhereYouAd.domain.example.domain.service.ExampleService;
import com.whereyouad.WhereYouAd.domain.example.presentation.docs.ExampleControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.response.DefaultIdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
