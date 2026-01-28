package com.whereyouad.WhereYouAd.domains.user.persistence.repository;

import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthProviderAccountRepository extends JpaRepository<AuthProviderAccount, Long> {
    AuthProviderAccount findByProviderId(String username);
}
