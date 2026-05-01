package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import java.util.Optional;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {
    Optional<PlatformAccount> findByExternalAccountIdAndProvider(String externalAccountId, Provider provider);
}
