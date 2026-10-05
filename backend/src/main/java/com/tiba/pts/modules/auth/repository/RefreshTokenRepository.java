package com.tiba.pts.modules.auth.repository;

import com.tiba.pts.modules.auth.domain.entity.RefreshToken;
import com.tiba.pts.modules.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /** JOIN FETCH : evite un SELECT supplementaire (N+1) lors de l'acces a l'utilisateur. */
  @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.token = :tokenHash")
  Optional<RefreshToken> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

  /**
   * Revocation atomique conditionnelle (compare-and-set) : sous acces concurrent, une seule
   * transaction obtient 1 ligne modifiee ; les autres obtiennent 0 et sont rejetees.
   */
  @Modifying
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.id = :id AND rt.revoked = false")
  int revokeIfActive(@Param("id") Long id);

  /** Logout en une seule requete (au lieu de SELECT + UPDATE), idempotent. */
  @Modifying
  @Query(
      "UPDATE RefreshToken rt SET rt.revoked = true "
          + "WHERE rt.token = :tokenHash AND rt.revoked = false")
  int revokeByTokenHash(@Param("tokenHash") String tokenHash);

  @Modifying
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
  int revokeAllByUser(@Param("user") User user);

  /**
   * Purge : tokens expires + tokens revoques crees avant la fenetre de retention.
   * Les tokens revoques recents sont conserves volontairement pour permettre la detection
   * de reutilisation.
   */
  @Modifying
  @Query(
      "DELETE FROM RefreshToken rt "
          + "WHERE rt.expiryDate < :now "
          + "OR (rt.revoked = true AND rt.createdDate < :revokedCutoff)")
  int deleteExpiredOrStaleRevokedTokens(
      @Param("now") Instant now, @Param("revokedCutoff") LocalDateTime revokedCutoff);
}
