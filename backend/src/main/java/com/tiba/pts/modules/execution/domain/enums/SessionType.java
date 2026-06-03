package com.tiba.pts.modules.execution.domain.enums;

public enum SessionType {
  REGULAR, // Issu de l'emploi du temps normal
  REPLACEMENT, // Cours de rattrapage
  SUPPORT,
  EXAM_SUPERVISION // Séance de surveillance d'examen (pas de cours enseigné)
}
