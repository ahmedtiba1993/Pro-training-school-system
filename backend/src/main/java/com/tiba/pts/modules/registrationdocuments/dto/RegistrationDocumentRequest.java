package com.tiba.pts.modules.registrationdocuments.dto;

import com.tiba.pts.modules.registrationdocuments.domain.enums.DocumentCondition;
import com.tiba.pts.modules.registrationdocuments.domain.enums.DocumentNature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;

@Data
public class RegistrationDocumentRequest {

  @NotBlank(message = "NAME_REQUIRED")
  private String name;

  @NotNull(message = "QUANTITY_REQUIRED")
  private Integer quantity;

  @NotNull(message = "TYPE_REQUIRED")
  private DocumentNature nature;

  @NotNull(message = "CONDITION_REQUIRED")
  private DocumentCondition condition;

  @NotEmpty(message = "LEVEL_IDS_REQUIRED")
  private Set<Long> levelIds;

  @NotNull(message = "MANDATORY_REQUIRED")
  private Boolean mandatory;
}
