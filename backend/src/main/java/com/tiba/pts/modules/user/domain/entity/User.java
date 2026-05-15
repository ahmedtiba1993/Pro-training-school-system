package com.tiba.pts.modules.user.domain.entity;

import com.tiba.pts.modules.profiles.domain.entity.Person;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "users")
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @ColumnDefault("'PENDING'")
  private UserStatus status;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id", referencedColumnName = "id", unique = true)
  private Person person;

  @Column(nullable = false)
  @ColumnDefault("true")
  private boolean forcePasswordChange;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(role.name()));
  }

  public boolean isAccountNonExpired() {
    return status != UserStatus.ARCHIVED;
  }

  @Override
  public boolean isAccountNonLocked() {
    return status != UserStatus.SUSPENDED;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return status == UserStatus.ACTIVE || status == UserStatus.PENDING;
  }
}
