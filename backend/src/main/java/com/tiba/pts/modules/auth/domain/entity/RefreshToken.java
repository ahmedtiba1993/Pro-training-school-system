package com.tiba.pts.modules.auth.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
      @Index(name = "idx_refresh_token_token", columnList = "token"),
      @Index(name = "idx_refresh_token_user_id", columnList = "user_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RefreshToken extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auth_refresh_token_seq")
  @SequenceGenerator(
      name = "auth_refresh_token_seq",
      sequenceName = "auth_refresh_token_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "expiry_date", nullable = false)
  private Instant expiryDate;

  @Column(nullable = false)
  private boolean revoked;
}
