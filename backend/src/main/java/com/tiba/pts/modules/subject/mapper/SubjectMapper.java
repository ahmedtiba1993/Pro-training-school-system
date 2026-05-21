package com.tiba.pts.modules.subject.mapper;

import com.tiba.pts.modules.profiles.mapper.RefTeacherSpecialtyMapper;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.subject.dto.request.SubjectRequest;
import com.tiba.pts.modules.subject.dto.response.SubjectResponse;
import com.tiba.pts.modules.subject.dto.response.SubjectShortResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = {RefTeacherSpecialtyMapper.class},
    builder = @Builder(disableBuilder = true))
public interface SubjectMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "totalHours", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  Subject toEntity(SubjectRequest request);

  @Mapping(source = "training.id", target = "trainingId")
  @Mapping(source = "training", target = "trainingLabel")
  SubjectResponse toResponse(Subject entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "totalHours", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  void updateEntityFromRequest(SubjectRequest request, @MappingTarget Subject entity);

  List<SubjectShortResponse> toShortResponseList(List<Subject> subjects);

  // This method will be called automatically to generate the label
  default String mapTrainingToLabel(Training training) {
    if (training == null) {
      return null;
    }

    String levelLabel = (training.getLevel() != null) ? training.getLevel().getCode() : "";
    String specialtyLabel =
        (training.getSpecialty() != null) ? training.getSpecialty().getLabel() : "";

    if (levelLabel.isEmpty() && specialtyLabel.isEmpty()) {
      return null;
    }

    return (levelLabel + " " + specialtyLabel).trim();
  }
}
