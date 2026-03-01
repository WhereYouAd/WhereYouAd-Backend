package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.AdvertisementQueryService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.AdvertisementControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/advertisement")
public class AdvertisementController implements AdvertisementControllerDocs {

    private final AdvertisementQueryService advertisementQueryService;

    @GetMapping("/{projectId}/rankings/roas")
    public ResponseEntity<DataResponse<AdvertisementResponse.RankingROASList>> getRoasRanking(
            @AuthenticationPrincipal (expression = "userId") Long userId,
            @PathVariable Long projectId,
            @Valid @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Valid @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
    {
        // 조회 기간 설정이 없으면 오늘부터 최근 1개월 전 데이터 조회
        LocalDate adjustedEnd = (endDate != null) ? endDate : LocalDate.now();
        LocalDate adjustedStart = (startDate != null) ? startDate : adjustedEnd.minusMonths(1);

        AdvertisementResponse.RankingROASList response = advertisementQueryService.getRoasRanking(
                userId, projectId, adjustedStart, adjustedEnd);

        return ResponseEntity.ok(DataResponse.from(response));
    }
}