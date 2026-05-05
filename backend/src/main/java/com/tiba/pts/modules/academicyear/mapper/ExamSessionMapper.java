package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.dto.request.ExamSessionRequest;
import com.tiba.pts.modules.academicyear.dto.response.ExamSessionResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExamSessionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "period", source = "periodId", qualifiedByName = "idToPeriod")
  ExamSession toEntity(ExamSessionRequest request);

  ExamSessionResponse toResponse(ExamSession entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "period", ignore = true)
  void updateEntityFromRequest(ExamSessionRequest request, @MappingTarget ExamSession entity);

  @Named("idToPeriod")
  default Period idToPeriod(Long id) {
    if (id == null) return null;
    Period period = new Period();
    period.setId(id);
    return period;
  }
}
