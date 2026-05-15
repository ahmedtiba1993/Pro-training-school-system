package com.tiba.pts.modules.user.repository;

import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  Optional<User> findByPersonId(Long personId);

  /**
   * Global paginated retrieval with optional filters. Resolves the N+1 SELECT issue via
   * EntityGraph.
   */
  @EntityGraph(attributePaths = {"person"})
  @Query(
      "SELECT u FROM User u LEFT JOIN u.person p WHERE "
          + "(:keyword IS NULL OR "
          + "  LOWER(p.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "  LOWER(p.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "  LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + ") "
          + "AND (:role IS NULL OR u.role = :role) "
          + "AND (:status IS NULL OR u.status = :status)")
  Page<User> findAllFiltered(
      @Param("keyword") String keyword,
      @Param("role") Role role,
      @Param("status") UserStatus status,
      Pageable pageable);
}
