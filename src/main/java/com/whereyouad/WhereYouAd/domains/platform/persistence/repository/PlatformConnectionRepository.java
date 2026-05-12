package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;
import java.util.Optional;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, Long> {

    // 특정 플랫폼의 모든 연동 정보 조회 (스케줄러용)
    List<PlatformConnection> findByPlatformAccount_Provider(Provider provider);

    // PlatformAccount와 Organization을 한 번에 가져오는 상세 조회 (LazyInitializationException 방지용)
    @Query("SELECT pc FROM PlatformConnection pc " +
            "JOIN FETCH pc.platformAccount pa " +
            "JOIN FETCH pa.organization " +
            "WHERE pc.id = :id")
    Optional<PlatformConnection> findWithAccountAndOrgById(@Param("id") Long id);

    // 조직 ID와 플랫폼으로 등록된 연동 정보(Account/Connection) 목록 조회
    List<PlatformConnection> findByPlatformAccount_Organization_IdAndPlatformAccount_Provider(Long orgId, Provider provider);

    // 사용자 ID와 플랫폼으로 등록된 연동 정보 목록 조회
    List<PlatformConnection> findByUser_IdAndPlatformAccount_Provider(Long userId, Provider provider);

    // 플랫폼으로 등록된 모든 연동 정보 목록 조회 (스케줄러용)
    List<PlatformConnection> findByPlatformAccount_Provider(Provider provider);

    // 유저 ID와 플랫폼 계정 ID로 연동 정보 조회 (중복 확인용)
    Optional<PlatformConnection> findByUser_IdAndPlatformAccount_Id(Long userId, Long platformAccountId);

    // 특정 Provider의 모든 Connection 조회 (Meta 스케줄러에서 사용)
    List<PlatformConnection> findAllByPlatformAccount_Provider(Provider provider);

    @Query("SELECT c.platformAccount.organization.id FROM PlatformConnection c WHERE c.platformAccount.provider = :provider")
    List<Long> findOrganizationIdsByProvider(@Param("provider") Provider provider);

    @Query("SELECT c FROM PlatformConnection c WHERE c.user.id = :userId " + "AND c.platformAccount.id = :platformAccountId")
    Optional<PlatformConnection> findByUserIdAndPlatformAccountId(@Param("userId") Long userId, @Param("platformAccountId") Long platformAccountId);
}
