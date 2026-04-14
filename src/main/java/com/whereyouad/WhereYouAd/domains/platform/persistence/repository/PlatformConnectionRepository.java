package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, Long> {
    
    // 조직 ID와 플랫폼으로 등록된 연동 정보(Account/Connection) 목록 조회
    List<PlatformConnection> findByPlatformAccount_Organization_IdAndPlatformAccount_Provider(Long orgId, Provider provider);

    // 특정 플랫폼의 모든 연동 정보 조회 (스케줄러용)
    List<PlatformConnection> findByPlatformAccount_Provider(Provider provider);
}
