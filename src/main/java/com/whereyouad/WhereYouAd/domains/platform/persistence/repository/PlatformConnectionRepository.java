package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, Long> {

    // 특정 플랫폼의 모든 연동 정보 조회 (스케줄러용)
    List<PlatformConnection> findByPlatformAccount_Provider(Provider provider);

    // PlatformAccount와 Organization을 한 번에 가져오는 상세 조회 (LazyInitializationException 방지용)
    @Query("SELECT pc FROM PlatformConnection pc " +
            "JOIN FETCH pc.platformAccount pa " +
            "JOIN FETCH pa.organization " +
            "WHERE pc.id = :id")
    Optional<PlatformConnection> findWithAccountAndOrgById(@Param("id") Long id);
}
