package com.tiba.pts.modules.profiles.service;

import com.tiba.pts.modules.profiles.domain.entity.Parent;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.domain.entity.StudentParent;
import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import com.tiba.pts.modules.profiles.dto.request.ExistenceCheckResponse;
import com.tiba.pts.modules.profiles.dto.request.ParentRequest;
import com.tiba.pts.modules.profiles.dto.response.ParentResponse;
import com.tiba.pts.modules.profiles.mapper.ParentMapper;
import com.tiba.pts.modules.profiles.repository.ParentRepository;
import com.tiba.pts.modules.profiles.repository.PersonRepository;
import com.tiba.pts.modules.profiles.repository.StudentParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentService {

  private final ParentRepository parentRepository;
  private final StudentParentRepository studentParentRepository;
  private final ParentMapper parentMapper;
  private final PersonRepository personRepository;

  @Transactional
  public Parent createParentEntity(ParentRequest request) {
    Parent parent = parentMapper.toEntity(request);
    return parentRepository.save(parent);
  }

  @Transactional
  public void linkStudentToParent(
      Student student, Parent parent, ParentalLink link, boolean isGuardian) {
    StudentParent studentParent =
        StudentParent.builder()
            .student(student)
            .parent(parent)
            .link(link)
            .isLegalGuardian(isGuardian)
            .build();

    studentParentRepository.save(studentParent);
  }

  @Transactional(readOnly = true)
  public List<ParentResponse> searchParents(String keyword) {
    List<Parent> parents = parentRepository.searchParentsAsList(keyword);

    return parents.stream().map(parentMapper::toResponse).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ExistenceCheckResponse checkExistence(String email, String phone) {
    boolean emailExists = StringUtils.hasText(email) && personRepository.existsByEmail(email);
    boolean phoneExists = StringUtils.hasText(phone) && personRepository.existsByPhone(phone);

    return ExistenceCheckResponse.builder()
        .emailExists(emailExists)
        .phoneExists(phoneExists)
        .build();
  }
}
