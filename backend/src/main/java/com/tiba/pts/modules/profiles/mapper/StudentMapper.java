package com.tiba.pts.modules.profiles.mapper;

import com.tiba.pts.modules.profiles.domain.entity.Parent;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.dto.response.ParentResponse;
import com.tiba.pts.modules.profiles.dto.response.StudentResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface StudentMapper {
  Student toEntity(StudentRequest request);

  StudentResponse toResponse(Student entity);

  ParentResponse toParentResponse(Parent entity);
}
