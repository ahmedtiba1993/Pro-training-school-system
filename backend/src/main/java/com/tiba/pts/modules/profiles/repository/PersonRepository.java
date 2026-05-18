package com.tiba.pts.modules.profiles.repository;

import com.tiba.pts.modules.profiles.domain.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);

  boolean existsByCin(String cin);

  boolean existsByEmailAndIdNot(String email, Long id);

  boolean existsByCinAndIdNot(String cin, Long id);

  boolean existsByPhoneAndIdNot(String phone, Long id);
}
