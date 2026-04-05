package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {

    // Meta 연동 시 기존 PlatformAccount 중복 생성 방지 (Meta UPSERT 용)
    Optional<PlatformAccount> findByExternalAccountIdAndProvider(String externalAccountId, Provider provider);
}
