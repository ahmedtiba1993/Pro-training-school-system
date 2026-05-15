package com.tiba.pts.modules.profiles.mapper;

import com.tiba.pts.modules.profiles.domain.entity.Parent;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.dto.response.ParentResponse;
import com.tiba.pts.modules.profiles.dto.response.StudentListResponse;
import com.tiba.pts.modules.profiles.dto.response.StudentResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface StudentMapper {

  @Mapping(
      target = "parents",
      ignore = true) // Ignored because it is handled manually in the Service
  Student toEntity(StudentRequest request);

  StudentResponse toResponse(Student entity);

  ParentResponse toParentResponse(Parent entity);

  @Mapping(target = "studentContact", source = "phone")
  @Mapping(target = "guardianContact", expression = "java(extractGuardianContact(student))")
  StudentListResponse toListResponse(Student student);

  // Default method for null-safe extraction of tutor number
  default String extractGuardianContact(Student student) {
    if (student == null || student.getParents() == null || student.getParents().isEmpty()) {
      return null;
    }

    return student.getParents().stream()
        .filter(sp -> sp != null && sp.isLegalGuardian() && sp.getParent() != null)
        .map(sp -> sp.getParent().getPhone())
        .filter(phone -> phone != null && !phone.trim().isEmpty())
        .findFirst()
        .orElse(null);
  }
}
