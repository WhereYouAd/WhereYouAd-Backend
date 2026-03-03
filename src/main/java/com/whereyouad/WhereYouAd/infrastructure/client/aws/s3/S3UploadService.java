package com.whereyouad.WhereYouAd.infrastructure.client.aws.s3;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file) throws IOException {
        // 파일명 중복을 막기 위해 UUID 추가
        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // 파일의 메타데이터(타입 등) 설정
        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType(file.getContentType())
                .build();

        // S3 버킷에 업로드
        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucket, uniqueFileName, inputStream, metadata);
        }

        // 업로드된 이미지의 URL 반환 (DB에 저장될 값)
        return "http://whereyouad-s3.s3.ap-northeast-2.amazonaws.com/" + uniqueFileName;
    }
}
