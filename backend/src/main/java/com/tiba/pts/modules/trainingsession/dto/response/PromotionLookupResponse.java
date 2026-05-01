package com.tiba.pts.modules.trainingsession.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionLookupResponse {
  private Long id;
  private String name;
}
