package com.whereyouad.WhereYouAd.domains.user.persistence.repository;

import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuthProviderAccountRepository extends JpaRepository<AuthProviderAccount, Long> {
    AuthProviderAccount findByProviderId(String username);

    @Query("select apa from AuthProviderAccount apa where apa.user.email = :email")
    List<AuthProviderAccount> findByUserEmail(@Param(value = "email") String email);

    // 로그인한 소셜 제공자 계정에 OAuth 토큰을 저장하기 위한 조회
    Optional<AuthProviderAccount> findByUser_EmailAndProvider(
            String email,
            com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider provider
    );

    // 회원탈퇴 시 이미 연동 해제가 완료된 계정은 제외하고 조회
    List<AuthProviderAccount> findByUser_IdAndUnlinkedAtIsNull(Long userId);

    // userId 기준 AuthProviderAccount 일괄 Hard Delete (User Hard Delete 정리용)
    @Modifying
    @Query("DELETE FROM AuthProviderAccount apa WHERE apa.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
