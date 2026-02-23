package com.whereyouad.WhereYouAd.domains.user.persistence.repository;

import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthProviderAccountRepository extends JpaRepository<AuthProviderAccount, Long> {
    AuthProviderAccount findByProviderId(String username);

    @Query("select apa from AuthProviderAccount apa where apa.user.email = :email")
    List<AuthProviderAccount> findByUserEmail(@Param(value = "email") String email);
}
