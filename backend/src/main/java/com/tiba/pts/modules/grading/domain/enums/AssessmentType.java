package com.tiba.pts.modules.grading.domain.enums;

public enum AssessmentType {
  // --- CONTRÔLE CONTINU (Géré par le Professeur pendant ses cours) ---
  DS, // Devoir Surveillé (Contrôle écrit en classe)
  TP, // Travaux Pratiques (Note de labo ou d'atelier)
  ORAL, // Interrogation Orale / Exposé
  PROJECT, // Projet Maison / Dossier à rendre

  // --- EXAMENS OFFICIELS (Gérés par la Scolarité avec la Logistique) ---
  FINAL_EXAM, // Examen Principal de fin de semestre
  RETAKE // Examen de Rattrapage
}
