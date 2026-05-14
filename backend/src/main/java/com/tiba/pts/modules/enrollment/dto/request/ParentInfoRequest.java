package com.tiba.pts.modules.enrollment.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParentInfoRequest extends PersonInfoRequest {
  private String profession;
}
