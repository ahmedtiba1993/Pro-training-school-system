package com.tiba.pts.modules.profiles.mapper;

import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.profiles.dto.request.TeacherRequest;
import com.tiba.pts.modules.profiles.dto.response.TeacherResponse;
import com.tiba.pts.modules.profiles.dto.response.TeacherSimpleResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = {RefTeacherSpecialtyMapper.class},
    builder = @Builder(disableBuilder = true))
public interface TeacherMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "specialties", ignore = true)
  Teacher toEntity(TeacherRequest request);

  TeacherResponse toResponse(Teacher entity);

  List<TeacherSimpleResponse> toSimpleResponseList(List<Teacher> activeTeachers);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "specialties", ignore = true)
  void updateEntityFromRequest(TeacherRequest request, @MappingTarget Teacher entity);
}
