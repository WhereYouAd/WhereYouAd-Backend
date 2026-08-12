package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {

    void findAllByProvider(String provider);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND c.status = 'ON_GOING'")
    Long sumAllBudgetsByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("""
            SELECT new com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse$OngoingPlatformAdCount(
                c.provider, COUNT(c)
            )
            FROM AdCampaign c
            JOIN c.project p
            JOIN p.organization o
            WHERE o.id = :orgId
              AND c.status = :status
              AND c.startDate <= :endDate
              AND c.endDate >= :startDate
            GROUP BY c.provider
            """)
    List<DashboardResponse.OngoingPlatformAdCount> countOngoingAdsByProvider(
            @Param("orgId") Long orgId,
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND c.provider = :provider AND c.status = 'ON_GOING'")
    Long sumBudgetsByUserIdAndOrgIdAndProvider(@Param("userId") Long userId, @Param("orgId") Long orgId,
            @Param("provider") Provider provider);

    // 예산 소진 현황(전체/일일) 조회용: budgetType 기준 예산 합산. provider 가 null 이면 조직 전체 합산(통합 대시보드), 지정되면 해당 플랫폼만 합산(플랫폼 대시보드)
    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o " +
            "WHERE om.user.id = :userId AND o.id = :orgId AND c.status = 'ON_GOING' AND c.budgetType = :budgetType " +
            "AND (:provider IS NULL OR c.provider = :provider)")
    Long sumBudgetsByUserIdAndOrgIdAndBudgetType(@Param("userId") Long userId, @Param("orgId") Long orgId,
            @Param("budgetType") BudgetType budgetType, @Param("provider") Provider provider);

    // 위 합산에 실제로 포함된 provider 목록 (통합 대시보드에서 그룹에 속한 플랫폼 표시용)
    @Query("SELECT DISTINCT c.provider FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o " +
            "WHERE om.user.id = :userId AND o.id = :orgId AND c.status = 'ON_GOING' AND c.budgetType = :budgetType " +
            "AND (:provider IS NULL OR c.provider = :provider)")
    List<Provider> findDistinctProvidersByUserIdAndOrgIdAndBudgetType(@Param("userId") Long userId, @Param("orgId") Long orgId,
            @Param("budgetType") BudgetType budgetType, @Param("provider") Provider provider);

    @Query("SELECT new com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto$CampaignSummary(c.project.id, c.provider, c.budget) " +
            "FROM AdCampaign c WHERE c.project.id IN :projectIds")
    List<ProjectQueryDto.CampaignSummary> findCampaignSummariesByProjectIds(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT new com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto$CampaignSummary(c.project.id, c.provider, c.budget) " +
            "FROM AdCampaign c WHERE c.project.id = :projectId")
    List<ProjectQueryDto.CampaignSummary> findCampaignSummariesByProjectId(@Param("projectId") Long projectId);

    // 캠페인 상세(프로젝트 상세) 페이지의 플랫폼별 남은 예산 계산용: campaignId + provider + budgetType + budget 조회
    @Query("SELECT new com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto$CampaignBudgetInfo(c.id, c.provider, c.budgetType, c.budget) " +
            "FROM AdCampaign c WHERE c.project.id = :projectId")
    List<ProjectQueryDto.CampaignBudgetInfo> findCampaignBudgetInfoByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query("UPDATE AdCampaign a SET a.status = :status WHERE a.project.id = :projectId")
    void updateStatusByProjectId(@Param("projectId") Long projectId, @Param("status") Status status);

    @Modifying
    @Query("UPDATE AdCampaign a SET a.status = :status WHERE a.project.organization.id = :orgId")
    void updateStatusByOrganizationId(@Param("orgId") Long orgId, @Param("status") Status status);

    //provider 값과 orgId 값이 일치하고, project 가 null 인 AdCampaign 엔티티 리스트로 추출
    @Query("SELECT adc FROM AdCampaign adc WHERE adc.provider = :provider AND adc.project IS null AND adc.organization.id = :orgId")
    List<AdCampaign> findByOrgIdAndProviderWithNullProject(@Param("orgId") Long orgId, @Param("provider") Provider provider);

    // 외부 캠페인 ID + 플랫폼 계정으로 AdCampaign 조회
    Optional<AdCampaign> findByExternalCampaignIdAndPlatformAccount(String externalCampaignId, PlatformAccount platformAccount);

    Optional<AdCampaign> findByPlatformAccountAndExternalCampaignId(PlatformAccount platformAccount, String externalCampaignId);

    // PlatformAccount 연동 해제 시 사용
    List<AdCampaign> findByPlatformAccount(PlatformAccount platformAccount);

    // 연동 해제 후 빈 Project 정리 대상 식별용 (project null 인 AdCampaign 은 제외)
    @Query("SELECT DISTINCT c.project.id FROM AdCampaign c " +
            "WHERE c.platformAccount.id = :platformAccountId AND c.project IS NOT NULL")
    List<Long> findDistinctProjectIdsByPlatformAccountId(@Param("platformAccountId") Long platformAccountId);

    // 특정 Project 에 남아있는 AdCampaign 수 (빈 Project 판단용)
    long countByProject_Id(Long projectId);
}
