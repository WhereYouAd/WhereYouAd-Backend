package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {

    // DISCONNECTED 인 PlatformAccount id 목록 조회 (연동 해제 정리 스케줄러용)
    @Query("SELECT pa.id FROM PlatformAccount pa WHERE pa.status = :status")
    List<Long> findIdsByStatus(@Param("status") PlatformStatus status);

    Optional<PlatformAccount> findByExternalAccountIdAndProviderAndOrganization_Id(String externalAccountId, Provider provider, Long orgId);

    // 계정이 존재하는지 확인
    boolean existsByExternalAccountIdAndOrganizationIdAndProvider(
            String externalAccountId, Long organizationId, Provider provider);

    // 조직 무관 조회 (동기화 등 내부 용도)
    Optional<PlatformAccount> findByExternalAccountIdAndProvider(String externalAccountId, Provider provider);

    // 조직을 포함한 조회 — 조직별 PlatformAccount 격리를 위해 UPSERT 시 사용
    Optional<PlatformAccount> findByExternalAccountIdAndProviderAndOrganization(String externalAccountId, Provider provider, Organization organization);
}
