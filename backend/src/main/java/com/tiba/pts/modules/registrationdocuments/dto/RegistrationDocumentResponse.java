package com.tiba.pts.modules.registrationdocuments.dto;

import com.tiba.pts.modules.registrationdocuments.domain.enums.DocumentCondition;
import com.tiba.pts.modules.registrationdocuments.domain.enums.DocumentNature;
import com.tiba.pts.modules.specialty.dto.LevelDto;
import lombok.Data;

import java.util.Set;

@Data
public class RegistrationDocumentResponse {

  private Long id;
  private String name;
  private Integer quantity;
  private DocumentNature nature;
  private DocumentCondition condition;
  private Set<LevelDto> levels;
  private boolean isMandatory;
}
