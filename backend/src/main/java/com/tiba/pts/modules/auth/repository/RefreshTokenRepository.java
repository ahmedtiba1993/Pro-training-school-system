package com.tiba.pts.modules.auth.repository;

import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.token = :token")
  Optional<RefreshToken> findByTokenWithUser(@Param("token") String token);

  @Modifying
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
  int revokeAllByUser(@Param("user") User user);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now OR rt.revoked = true")
  int deleteExpiredOrRevokedTokens(@Param("now") Instant now);
}
