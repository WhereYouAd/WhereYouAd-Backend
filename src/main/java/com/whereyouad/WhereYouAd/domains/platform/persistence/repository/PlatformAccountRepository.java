package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {

    // 계정이 존재하는지 확인
    boolean existsByExternalAccountIdAndOrganizationIdAndProvider(
            String externalAccountId, Long organizationId, Provider provider);
}
