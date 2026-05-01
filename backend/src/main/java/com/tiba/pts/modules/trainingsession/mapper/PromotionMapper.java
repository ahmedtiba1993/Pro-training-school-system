package com.tiba.pts.modules.trainingsession.mapper;

import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionLookupResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PromotionMapper {

  PromotionLookupResponse toLookupResponse(Promotion promotion);
}
