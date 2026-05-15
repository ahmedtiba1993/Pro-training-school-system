package com.tiba.pts.modules.profiles.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.Year;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraduationRecord {

  private String degreeName;
  private Year graduationYear;
  private String mention;
}
