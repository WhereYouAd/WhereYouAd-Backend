package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.application.mapper.DashboardConverter;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.DashboardException;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.code.DashBoardErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    // 지정된 날짜(startDate ~ endDate)동안의 진행 중(ON_GOING) 상태인 광고 개수를 찾는 메서드
    public DashboardResponse.OngoingPlatformAdCountResponse getOngoingAdCountByProvider(
        Long userId, Long orgId, LocalDate startDate, LocalDate endDate) {

        // 1. 날짜 유효성 검사
        // 시작일이나 종료일이 미래인 경우
        if (startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now())) {
            throw new DashboardException(DashBoardErrorCode.INVALID_DATE_RANGE);
        }
        // 시작일보다 종료일이 더 빠른 경우
        if (startDate.isAfter(endDate)) {
            throw new DashboardException(DashBoardErrorCode.INVALID_DATE_RANGE);
        }

        // 2. 조직 존재 여부 확인
        orgRepository.findById(orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_NOT_FOUND));

        // 3. 유저가 해당 조직 멤버인지 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new DashboardException(DashBoardErrorCode.ACCESS_FORBIDDEN));

        // 4. 진행 중인 광고 개수 세기
        List<DashboardResponse.OngoingPlatformAdCount> providerCount = adCampaignRepository
                .countOngoingAdsByProvider(orgId, Status.ON_GOING, startDate, endDate);

        // 5. 변환 후 반환
        return DashboardConverter.toOngoingPlatformAdCountResponse(providerCount, startDate, endDate);
    }
}