package com.tiba.pts.modules.person.repository;

import com.tiba.pts.modules.person.domain.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
  boolean existsByEmail(String email);

  boolean existsByCin(String cin);
}
