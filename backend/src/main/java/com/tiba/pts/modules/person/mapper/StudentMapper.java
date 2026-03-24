package com.tiba.pts.modules.person.mapper;

import com.tiba.pts.modules.person.domain.entity.Student;
import com.tiba.pts.modules.person.dto.request.StudentRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface StudentMapper {
  Student toEntity(StudentRequest request);
}
