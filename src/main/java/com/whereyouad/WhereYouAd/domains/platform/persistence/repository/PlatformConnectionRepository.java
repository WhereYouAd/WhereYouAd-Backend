package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, Long> {
    
    // 조직 ID와 플랫폼으로 등록된 연동 정보(Account/Connection) 목록 조회
    List<PlatformConnection> findByPlatformAccount_Organization_IdAndPlatformAccount_Provider(Long orgId, Provider provider);

    // 특정 Provider의 모든 Connection 조회 (Meta 스케줄러에서 사용)
    List<PlatformConnection> findAllByPlatformAccount_Provider(Provider provider);

    @Query("SELECT c.platformAccount.organization.id FROM PlatformConnection c WHERE c.platformAccount.provider = :provider")
    List<Long> findOrganizationIdsByProvider(@Param("provider") Provider provider);
}
