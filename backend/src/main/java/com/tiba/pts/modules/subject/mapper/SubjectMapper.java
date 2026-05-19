package com.tiba.pts.modules.subject.mapper;

import com.tiba.pts.modules.profiles.mapper.RefTeacherSpecialtyMapper;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.subject.dto.request.SubjectRequest;
import com.tiba.pts.modules.subject.dto.response.SubjectResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = "spring",
    uses = {RefTeacherSpecialtyMapper.class},
    builder = @Builder(disableBuilder = true))
public interface SubjectMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "totalHours", ignore = true)
  @Mapping(target = "specialty", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  Subject toEntity(SubjectRequest request);

  @Mapping(source = "specialty.id", target = "specialtyId")
  @Mapping(source = "specialty.label", target = "specialtyLabel")
  SubjectResponse toResponse(Subject entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "totalHours", ignore = true)
  @Mapping(target = "specialty", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  void updateEntityFromRequest(SubjectRequest request, @MappingTarget Subject entity);
}
