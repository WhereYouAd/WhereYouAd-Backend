package com.whereyouad.WhereYouAd.domains.user.persistence.repository;

import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AuthProviderAccountRepository extends JpaRepository<AuthProviderAccount, Long> {
    AuthProviderAccount findByProviderId(String username);

    @Query("select apa from AuthProviderAccount apa where apa.user = :user")
    Optional<AuthProviderAccount> findByUser(@Param(value = "user") User user);
}
