package com.tiba.pts.modules.profiles.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParentRequest extends PersonRequest {

  private String profession;
}
