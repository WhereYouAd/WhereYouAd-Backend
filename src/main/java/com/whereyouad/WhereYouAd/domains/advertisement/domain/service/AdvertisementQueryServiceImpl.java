package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementException;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.RoasProjection;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.project.exception.code.ProjectErrorCode;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.domains.project.persistence.repository.ProjectRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvertisementQueryServiceImpl implements AdvertisementQueryService{

    private final MetricFactRepository metricFactRepository;
    private final ProjectRepository projectRepository;
    private final OrgMemberRepository orgMemberRepository;

    @Override
    public AdvertisementResponse.RankingROASList getRoasRanking(Long userId, Long projectId, LocalDate startDate, LocalDate endDate) {

        // 1. 날짜 유효성 검사 (시작일보다 종료일이 더 빠른 경우)
        if (startDate.isAfter(endDate)) {
            throw new AdvertisementException(AdvertisementErrorCode.INVALID_DATE_RANGE);
        }

        // 2. 프로젝트 존재 여부 확인
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AdvertisementException(ProjectErrorCode.PROJECT_NOT_FOUND));

        // 3. 요청 맴버의 해당 프로젝트에 대한 접근 권한 체크 (다른 조직의 프로젝트 접근 시도)
        boolean isMember = orgMemberRepository.existsByUserIdAndOrganizationId(userId, project.getOrganization().getId());
        if (!isMember) {
            throw new AdvertisementException(ProjectErrorCode.ACCESS_FORBIDDEN);
        }

        // 4. 현재 기간 성과 조회
        List<RoasProjection> current = metricFactRepository.findRoasByProjectAndPeriod(
                projectId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));

        // 해당 기간에 데이터 없는 경우 -> 빈 목록 반환
        if (current.isEmpty()) {
            return new AdvertisementResponse.RankingROASList(startDate, endDate, List.of());
        }

        // 5. 이전 기간 날짜 구하기 (startDate, endDate만큼의 기간을 구하여 뺌)
        // 조회한 기간의 총 일수 구하기 (시작일 포함이므로 +1)
        long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        // 직전 동일 기간 구하기 (각각 총 일수만큼 그대로 빼기)
        LocalDate prevStart = startDate.minusDays(periodDays);
        LocalDate prevEnd = endDate.minusDays(periodDays);

        // 6. 이전 동일 기간 성과 조회 (diffRate 계산을 위함)
        List<RoasProjection> previous = metricFactRepository.findRoasByProjectAndPeriod(
                projectId,
                prevStart.atStartOfDay(),
                prevEnd.atTime(23, 59, 59));

        // 7. 이전 기간 결과를 Map<String(provider), Double(roas)> 로 변환
        Map<String, Double> prevRoasMap = previous.stream()
                .collect(Collectors.toMap(
                        RoasProjection::getProvider,
                        proj -> calculateRoas(proj.getTotalRevenue(), proj.getTotalSpend())));

        // 8. 현재 기간 결과를 ROAS 기준 내림차순 정렬
        List<RoasProjection> sorted = current.stream()
                .sorted(Comparator.comparingDouble(
                        (RoasProjection p) -> calculateRoas(p.getTotalRevenue(), p.getTotalSpend())).reversed())
                .toList();

        // 9. DTO 변환 + 순위(rank) + diffRate 계산
        List<AdvertisementResponse.RankingROAS> rankings = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {

            // i번째(순위) RoasProjection 값
            RoasProjection proj = sorted.get(i);

            // 해당 RoasProjection에 대한 값들
            Double currentRoas = calculateRoas(proj.getTotalRevenue(), proj.getTotalSpend()); // 현재 roas
            Double prevRoas = prevRoasMap.get(proj.getProvider()); // 이전 roas
            Integer diffRate = computeDiffRate(currentRoas, prevRoas); // 변화율 계산
            Provider provider = Provider.valueOf(proj.getProvider()); // String -> Enum 변환

            rankings.add(AdvertisementConverter.toRankingROAS(
                    i + 1,
                    provider,
                    currentRoas,
                    diffRate,
                    proj.getTotalRevenue(),
                    proj.getTotalSpend()
            ));
        }

        return new AdvertisementResponse.RankingROASList(startDate, endDate, rankings);
    }




    // ROAS = (revenue / spend) * 100
    // spend가 null 또는 0이면 0.0 반환 (Division by Zero 방어)
    private double calculateRoas(BigDecimal revenue, BigDecimal spend) {
        // spend가 null이거나 0인 경우 -> 0.0 반환
        if (spend == null || spend.compareTo(BigDecimal.ZERO) == 0)
            return 0.0;

        // 매출이 없는 경우 -> 0.0 반환
        if (revenue == null)
            return 0.0;

        // ROAS 계산 후 반환
        return revenue.divide(spend, 4, RoundingMode.HALF_UP) // 소수점 4째자리 까지 구하고 반올림
                .multiply(BigDecimal.valueOf(100)) // * 100
                .doubleValue(); // double로 형변환
    }

    // 변화율 = ((current - prev) / prev) * 100 (반올림 정수)
    // prevRoas가 null이거나 0이면 비교 불가 -> null 반환
    private Integer computeDiffRate(double currentRoas, Double prevRoas) {
        // 이전 데이터가 없거나 0인 경우 null 반환
        if (prevRoas == null || prevRoas == 0.0)
            return null;

        // 변화율 계산 후 반환
        double rate = ((currentRoas - prevRoas) / prevRoas) * 100.0;
        return (int) Math.round(rate);
    }
}
