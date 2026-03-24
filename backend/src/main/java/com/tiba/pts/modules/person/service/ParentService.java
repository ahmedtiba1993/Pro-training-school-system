package com.tiba.pts.modules.person.service;

import com.tiba.pts.modules.person.domain.entity.Parent;
import com.tiba.pts.modules.person.domain.entity.Student;
import com.tiba.pts.modules.person.domain.enums.ParentType;
import com.tiba.pts.modules.person.dto.request.ParentRequest;
import com.tiba.pts.modules.person.mapper.ParentMapper;
import com.tiba.pts.modules.person.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParentService {

  private final ParentRepository parentRepository;
  private final ParentMapper parentMapper;

  @Transactional
  public Parent processParent(ParentRequest parentReq, ParentType type, Student student) {
    if (parentReq == null) {
      return null;
    }
    Parent parent = parentMapper.toEntity(parentReq);
    parent.setParentType(type);
    parent.getChildren().add(student);
    return parentRepository.save(parent);
  }
}
