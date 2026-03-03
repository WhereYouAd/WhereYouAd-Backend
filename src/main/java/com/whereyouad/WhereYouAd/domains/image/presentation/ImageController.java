package com.whereyouad.WhereYouAd.domains.image.presentation;

import com.whereyouad.WhereYouAd.infrastructure.client.aws.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3UploadService s3UploadService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestPart("image") MultipartFile image) {
        try {
            String imageUrl = s3UploadService.uploadImage(image);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("이미지 업로드에 실패했습니다.");
        }
    }
}
