package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementException;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.MetricSumProjection;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.application.mapper.DashboardConverter;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.DashboardException;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.code.DashboardErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.project.exception.code.ProjectErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AdCampaignRepository adCampaignRepository;
    private final MetricFactRepository metricFactRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgRepository orgRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.BudgetSummaryResponse getBudgetSummary(Long userId, Long orgId, String providerType) {

        // 조직 존재 여부 확인
        orgRepository.findById(orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_NOT_FOUND));

        // 유저가 해당 조직인지 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        Long totalBudget;
        BigDecimal totalSpendDec;

        // 통합 대시보드
        if (providerType == null || providerType.trim().isEmpty()) {
            totalBudget = adCampaignRepository.sumAllBudgetsByUserIdAndOrgId(userId, orgId);
            totalSpendDec = metricFactRepository.sumAllSpendsByUserIdAndOrgId(userId, orgId);
            providerType = "ALL";
        }
        // 플랫폼 대시보드
        else {
            Provider provider = Provider.valueOf(providerType.toUpperCase());
            totalBudget = adCampaignRepository.sumBudgetsByUserIdAndOrgIdAndProvider(userId, orgId, provider);
            totalSpendDec = metricFactRepository.sumSpendsByUserIdAndOrgIdAndProvider(userId, orgId, provider);
            providerType = provider.name();
        }

        // Null 방지
        totalBudget = (totalBudget != null) ? totalBudget : 0L;
        Long totalSpend = (totalSpendDec != null) ? totalSpendDec.longValue() : 0L;

        // 잔액 및 퍼센트
        Long remainingBudget = totalBudget - totalSpend;

        // 예산이 0원이면 0%, 아니면 (소진액 / 총예산 * 100) 값의 소수점 첫째 자리까지 반올림
        Double usagePercentage = (totalBudget == 0) ? 0.0
                : Math.round(((double) totalSpend / totalBudget) * 1000) / 10.0;

        return new DashboardResponse.BudgetSummaryResponse(
                providerType,
                usagePercentage,
                totalBudget,
                totalSpend,
                remainingBudget);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.AggregatedSummaryResponse getAggregatedMetrics(Long userId, Long orgId, String providerType) {
        //Organization 존재 확인
        if (!orgRepository.existsById(orgId)) {
            throw new AdvertisementException(OrgErrorCode.ORG_NOT_FOUND);
        }

        //해당 회원이 조직에 속하는지 확인
        boolean isMember = orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId);
        if (!isMember) {
            throw new DashboardException(ProjectErrorCode.ACCESS_FORBIDDEN);
        }

        //DB 내부 Mock data 중 가장 최근의 timeBucket 값 추출
        LocalDateTime latestDate = metricFactRepository.findLatestTimeBucket()
                .orElse(LocalDateTime.now());

        //DB 내부 Mock data 중 가장 최근의 timeBucket 값 기반 한달전, 두달전 기준 정립
        LocalDateTime oneMonthAgo = latestDate.minusMonths(1);

        LocalDateTime twoMonthsAgo = latestDate.minusMonths(2);

        MetricSumProjection currentProjection;
        MetricSumProjection pastProjection;

        if (providerType == null || providerType.trim().isEmpty()) { //providerType 입력 안됐으면, 조직 내 모든 데이터 집계
            //시간값들과 회원이 속한 project 리스트 기반 projection 으로 DB 에서
            //TotalImpressions, TotalClicks, TotalConversions, TotalSpend, TotalRevenue 를 집계해서 가져오기
            currentProjection = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                    orgId, oneMonthAgo, latestDate
            ); //가장 최근 ~ 한달 전의 집계 projection

            pastProjection = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                    orgId, twoMonthsAgo, oneMonthAgo
            ); //한달전 ~ 두달전의 집계 projection

        } else { //providerType 이 있다면, 해당 provider 데이터 집계

            Provider provider;
            try { //providerType 에 잘못된 값이 입력되지 않았는지 검증
                provider = Provider.valueOf(providerType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new DashboardException(DashboardErrorCode.PROVIDER_NOT_VALID);
            }
            currentProjection = metricFactRepository.findMetricsSumByOrgIdAndProvider(
                    orgId, provider, oneMonthAgo, latestDate);
            pastProjection = metricFactRepository.findMetricsSumByOrgIdAndProvider(
                    orgId, provider, twoMonthsAgo, oneMonthAgo);
        }

        //최근 ~ 한달전 ROAS 값 계산하기 -> revenue / spend * 100
        BigDecimal currTotalRevenue = toZeroIfNull(currentProjection.getTotalRevenue());
        BigDecimal currTotalSpend = toZeroIfNull(currentProjection.getTotalSpend());
        BigDecimal currentRoasBigDecimal = safePercent(currTotalRevenue, currTotalSpend);

        //한달전 ~ 두달전 ROAS 값 계산 -> revenue / spend * 100
        BigDecimal pastTotalRevenue = toZeroIfNull(pastProjection.getTotalRevenue());
        BigDecimal pastTotalSpend = toZeroIfNull(pastProjection.getTotalSpend());
        BigDecimal pastRoas = safePercent(pastTotalRevenue, pastTotalSpend);

        //전환율(CVR) 계산 -> totalConversions(전환수 합계) / totalClicks(클릭수 합계) * 100
        double rawCurrentCvr = safePercent(currentProjection.getTotalConversions(), currentProjection.getTotalClicks());
        double rawPastCvr = safePercent(pastProjection.getTotalConversions(), pastProjection.getTotalClicks());

        //각각의 지표값 -> 클릭수(totalClicks), 노출수(totalImpressions), 전환율(currentCvr), 광고비 대비 매출(ROAS)
        Long totalClicks = currentProjection.getTotalClicks() != null ? currentProjection.getTotalClicks() : 0L;
        Long totalImpressions = currentProjection.getTotalImpressions() != null ? currentProjection.getTotalImpressions() : 0L;
        double currentCvr = Math.floor(rawCurrentCvr * 100.0) / 100.0; //CVR 소수점 2번째자리까지만 파싱
        double currentRoas = currentRoasBigDecimal.doubleValue(); //ROAS 소수점 2번째자리까지만 파싱된 BigDecimal -> double 로 형변환

        //각 지표값의 변화율 -> 클릭수 변화율, 노출수 변화율, 전환율 변화율, ROAS 변화율
        Double clickChangeRate = calculateChangeRate(currentProjection.getTotalClicks(), pastProjection.getTotalClicks());
        Double impressionChangeRate = calculateChangeRate(currentProjection.getTotalImpressions(), pastProjection.getTotalImpressions());
        Double cvrChangeRate = calculateChangeRate(rawCurrentCvr, rawPastCvr);
        Double roasChangeRate = calculateChangeRate(currentRoasBigDecimal, pastRoas);

        return DashboardConverter.toAggregatedSummary(
                totalClicks,
                clickChangeRate,
                totalImpressions,
                impressionChangeRate,
                currentCvr,
                cvrChangeRate,
                currentRoas,
                roasChangeRate
                );
    }

    //변화율 계산 메서드
    private Double calculateChangeRate(Number current, Number past) {
        // null 방어 및 double 형변환
        double currentVal = (current != null) ? current.doubleValue() : 0.0;
        double pastVal = (past != null) ? past.doubleValue() : 0.0;

        // Divide by Zero 방어 (과거 데이터가 0인 경우)
        if (pastVal == 0.0) {
            if (currentVal > 0.0) {
                // 과거엔 0이었으나 현재 실적이 발생한 경우 (비즈니스 룰에 따라 100% 등으로 설정)
                return 100.0;
            }
            // 둘 다 0이거나 데이터가 아예 없는 경우
            return 0.0;
        }

        // 변화율 계산
        double changeRate = ((currentVal - pastVal) / pastVal) * 100.0;

        // 소수점 둘째 자리까지 버림 처리
        return BigDecimal.valueOf(changeRate)
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }

    //지표 값이 null 일 경우 BigDecimal 의 0 으로 바꿔주는 메서드
    private BigDecimal toZeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    //나눗셈 계산시 분모가 0 일 경우 나눗셈 시행하지 않고 0.00 반환
    private BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0 || numerator == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
        }
        return numerator.divide(denominator, 4, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.DOWN);
    }

    //나눗셈 계산시 분모가 0 일 경우 나눗셈 시행하지 않고 0.00 반환
    private double safePercent(Number numerator, Number denominator) {
        double n = numerator == null ? 0.0 : numerator.doubleValue();
        double d = denominator == null ? 0.0 : denominator.doubleValue();
        if (d == 0.0) return 0.0;
        return (n / d) * 100.0;
    }
}
