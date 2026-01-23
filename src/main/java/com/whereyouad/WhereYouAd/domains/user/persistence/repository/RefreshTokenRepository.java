package com.whereyouad.WhereYouAd.domains.user.persistence.repository;

import com.whereyouad.WhereYouAd.domains.user.persistence.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    @Query("select r from RefreshToken r where r.keyId = :keyId")
    Optional<RefreshToken> findByKeyId(@Param("keyId") String keyId);
}
