package com.tiba.pts.modules.classmanagement.mapper;

import com.tiba.pts.modules.classmanagement.domain.entity.ClassAssignment;
import com.tiba.pts.modules.classmanagement.dto.request.ClassAssignmentRequest;
import com.tiba.pts.modules.classmanagement.dto.response.ClassStudentResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ClassAssignmentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "enrollment", ignore = true)
  ClassAssignment toEntity(ClassAssignmentRequest request);

  @Mapping(target = "firstName", source = "enrollment.student.firstName")
  @Mapping(target = "lastName", source = "enrollment.student.lastName")
  @Mapping(target = "phone", source = "enrollment.student.phone")
  @Mapping(target = "studentCode", source = "enrollment.student.studentCode")
  @Mapping(target = "enrollmentStatus", source = "enrollment.status")
  @Mapping(target = "gender", source = "enrollment.student.gender")
  @Mapping(target = "enrollmentId", source = "enrollment.id")
  ClassStudentResponse toStudentResponse(ClassAssignment assignment);
}
