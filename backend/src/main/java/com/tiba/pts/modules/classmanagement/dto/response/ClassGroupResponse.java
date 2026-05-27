package com.tiba.pts.modules.classmanagement.dto.response;

import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassGroupResponse {
  private Long id;
  private String code;
  private String name;
  private Integer capacity;
  private Integer currentSize;
  private ClassStatus status;
  private Long promotionId;
  private String promotionName;
  private String promotionType;
}
