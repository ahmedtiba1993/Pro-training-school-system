package com.tiba.pts.modules.subject.domain.enums;

public enum SubjectStatus {
  DRAFT, // En cours de rédaction, pas encore visible pour les formations
  ACTIVE, // Validée, prête à être utilisée dans les programmes (Syllabus)
  ARCHIVED // Obsolète, n'apparait plus dans les listes, mais reste en BDD pour l'historique
}
