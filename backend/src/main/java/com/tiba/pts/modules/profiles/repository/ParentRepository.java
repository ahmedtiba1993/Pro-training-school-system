package com.tiba.pts.modules.profiles.repository;

import com.tiba.pts.modules.profiles.domain.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

  @Query(
      "SELECT p FROM Parent p WHERE "
          + "(:keyword IS NULL OR :keyword = '' OR "
          + "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(p.cin) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(p.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  List<Parent> searchParentsAsList(@Param("keyword") String keyword);
}
