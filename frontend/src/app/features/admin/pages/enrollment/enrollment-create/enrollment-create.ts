import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  LevelControllerService,
  RegistrationDocumentControllerService,
  SpecialtyControllerService,
  TrainingSessionControllerService
} from '../../../../../core/api';

@Component({
  selector: 'app-enrollment-create',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './enrollment-create.html'
})
export class EnrollmentCreate implements OnInit {
  private fb = inject(FormBuilder);
  private levelService = inject(LevelControllerService);
  private specialtyService = inject(SpecialtyControllerService);
  private sessionService = inject(TrainingSessionControllerService);
  private documentService = inject(RegistrationDocumentControllerService);

  // Signaux pour les listes
  levels = signal<any[]>([]);
  isLoadingLevels = signal<boolean>(false);

  specialties = signal<any[]>([]);
  isLoadingSpecialties = signal<boolean>(false);

  sessions = signal<any[]>([]);
  isLoadingSessions = signal<boolean>(false);

  // Signal pour mémoriser temporairement le niveau sélectionné hors du formulaire
  selectedLevelId = signal<number | null>(null);

  // --- NOUVEAUX SIGNAUX POUR LES DOCUMENTS ---
  documents = signal<any[]>([]);
  isLoadingDocuments = signal<boolean>(false);

  // Pour suivre les documents cochés (stocke les IDs)
  checkedDocumentIds = signal<Set<number>>(new Set());

  // Signal calculé pour le compteur (ex: "2/4")
  checkedCount = signal<number>(0);
  totalCount = computed(() => this.documents().length);

  // Le FormGroup NE CONTIENT PLUS QUE trainingSessionId
  enrollmentForm: FormGroup = this.fb.group({
    trainingSessionId: [{ value: '', disabled: true }, Validators.required],
    documents: this.fb.array([])
  });

  // 2. Getter pratique pour accéder au FormArray plus facilement
  get documentsFormArray(): FormArray {
    return this.enrollmentForm.get('documents') as FormArray;
  }

  ngOnInit(): void {
    this.loadLevels();
    this.documentsFormArray.valueChanges.subscribe(docs => {
      const providedCount = docs.filter((d: any) => d.isProvided === true).length;
      this.checkedCount.set(providedCount);
    });
  }

  loadLevels() {
    this.isLoadingLevels.set(true);
    this.levelService.getAllLevels().subscribe({
      next: (res: any) => {
        this.levels.set(res.data?.content || res.data || []);
        this.isLoadingLevels.set(false);
      },
      error: err => {
        console.error('Erreur chargement niveaux', err);
        this.isLoadingLevels.set(false);
      }
    });
  }

  // --- NOUVEAU : Gère le clic sur le menu Niveau ---
  onLevelChange(event: Event) {
    const target = event.target as HTMLSelectElement;
    const levelId = target.value ? Number(target.value) : null;

    this.selectedLevelId.set(levelId);

    // Réinitialisation de tout
    this.specialties.set([]);
    this.sessions.set([]);
    this.documents.set([]); // On vide la liste des documents
    this.checkedDocumentIds.set(new Set()); // On vide les cases cochées
    this.enrollmentForm.get('trainingSessionId')?.disable();
    this.enrollmentForm.get('trainingSessionId')?.setValue('');

    if (levelId) {
      this.loadSpecialties(levelId);
      this.loadDocuments(levelId); // <-- Appel API pour les documents
    }
  }

  // --- NOUVEAU : Gère le clic sur le menu Spécialité ---
  onSpecialtyChange(event: Event) {
    const target = event.target as HTMLSelectElement;
    const specialtyId = target.value ? Number(target.value) : null;
    const levelId = this.selectedLevelId(); // On récupère le niveau mémorisé

    // On réinitialise les sessions
    this.sessions.set([]);
    this.enrollmentForm.get('trainingSessionId')?.disable();
    this.enrollmentForm.get('trainingSessionId')?.setValue('');

    if (specialtyId && levelId) {
      this.loadSessions(levelId, specialtyId);
    }
  }

  loadSpecialties(levelId: number) {
    this.isLoadingSpecialties.set(true);
    this.specialtyService.getSpecialtiesByLevel(levelId).subscribe({
      next: (res: any) => {
        this.specialties.set(res.data || []);
        this.isLoadingSpecialties.set(false);
      },
      error: err => {
        console.error('Erreur chargement spécialités', err);
        this.isLoadingSpecialties.set(false);
      }
    });
  }

  loadSessions(levelId: number, specialtyId: number) {
    this.isLoadingSessions.set(true);
    // On bloque le champ du formulaire pendant le chargement
    this.enrollmentForm.get('trainingSessionId')?.disable();

    this.sessionService.getSessionsByLevelAndSpecialty(levelId, specialtyId).subscribe({
      next: (res: any) => {
        this.sessions.set(res.data || []);
        this.isLoadingSessions.set(false);
        this.enrollmentForm.get('trainingSessionId')?.enable(); // On libère le champ
      },
      error: err => {
        console.error('Erreur chargement sessions', err);
        this.isLoadingSessions.set(false);
      }
    });
  }

  // --- NOUVELLE MÉTHODE POUR CHARGER LES DOCUMENTS ---
  loadDocuments(levelId: number) {
    this.isLoadingDocuments.set(true);

    this.documentService.getDocumentsByLevel(levelId).subscribe({
      next: (res: any) => {
        const fetchedDocs = res.data || [];
        this.documents.set(fetchedDocs);

        // On vide le tableau au cas où l'utilisateur change de niveau
        this.documentsFormArray.clear();

        // Pour chaque document, on crée un groupe { registrationDocumentId, isProvided }
        fetchedDocs.forEach((doc: any) => {
          this.documentsFormArray.push(
            this.fb.group({
              registrationDocumentId: [doc.id, Validators.required],
              isProvided: [false, Validators.required] // Non coché par défaut
            })
          );
        });

        this.isLoadingDocuments.set(false);
      },
      error: err => {
        console.error('Erreur chargement documents', err);
        this.isLoadingDocuments.set(false);
      }
    });
  }

  // NOUVEAU : Méthode pour voir le résultat JSON final à envoyer
  onSubmit() {
    // getRawValue() récupère tout, même les champs "disabled"
    const payload = this.enrollmentForm.getRawValue();

    console.log('=== PAYLOAD POUR SPRING BOOT ===');
    console.log(JSON.stringify(payload, null, 2)); // Affichage formaté et lisible

    // Petite vérification pour vous aider à déboguer
    if (this.enrollmentForm.invalid) {
      console.warn('⚠️ Attention : Le formulaire est actuellement invalide.');
      console.log('Vérifiez que vous avez bien sélectionné une session de formation.');
    } else {
      console.log("✅ Formulaire valide ! Prêt pour l'envoi.");
    }
  }
}
