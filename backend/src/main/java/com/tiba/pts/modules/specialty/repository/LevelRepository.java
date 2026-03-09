package com.tiba.pts.modules.specialty.repository;

import com.tiba.pts.modules.specialty.domain.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, Long id);
}
