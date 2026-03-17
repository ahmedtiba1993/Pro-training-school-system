import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import {
  AcademicYearControllerService,
  AcademicYearDto,
  LevelControllerService,
  LevelDto,
  SpecialtyControllerService,
  SpecialtyResponse,
  TrainingSessionControllerService,
  TrainingSessionResponse,
  TrainingSessionRequest
} from '../../../../core/api';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../shared/services/toast.service';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-training-session',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, PaginationComponent],
  templateUrl: './training-session.html',
  styleUrl: './training-session.css'
})
export class TrainingSession implements OnInit {
  // --- SERVICE INJECTIONS ---
  private academicYearService = inject(AcademicYearControllerService);
  private levelService = inject(LevelControllerService);
  private specialtyService = inject(SpecialtyControllerService);
  private trainingSessionService = inject(TrainingSessionControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  // --- SIGNALS: Academic Year ---
  activeYear = signal<AcademicYearDto | null>(null);
  isLoadingYear = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // --- SIGNALS: Levels ---
  levels = signal<LevelDto[]>([]);
  isLoadingLevels = signal<boolean>(true);
  levelsError = signal<string | null>(null);
  selectedLevelId = signal<number | null>(null);

  // --- SIGNALS: Specialties ---
  specialties = signal<SpecialtyResponse[]>([]);
  isLoadingSpecialties = signal<boolean>(false);
  specialtiesError = signal<string | null>(null);
  selectedSpecialtyId = signal<number | null>(null);

  // --- SIGNALS: Sessions Table (Promotions) ---
  trainingSessions = signal<TrainingSessionResponse[]>([]);
  isLoadingSessions = signal<boolean>(true);

  // --- SIGNALS: In Progress Promotions (Cards) ---
  inProgressSessions = signal<TrainingSessionResponse[]>([]);
  isLoadingInProgress = signal<boolean>(true);

  // --- SIGNALS: Modals ---
  isModalOpen = signal<boolean>(false);
  selectedSession = signal<TrainingSessionResponse | null>(null);
  editingSession = signal<TrainingSessionResponse | null>(null);

  // FORM DECLARATION
  promotionForm!: FormGroup;
  isSaving = signal<boolean>(false);

  // --- PAGINATION ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(5);
  totalElements = signal<number>(0);

  // FORM INITIALIZATION
  initForm() {
    this.promotionForm = this.fb.group({
      promotionName: ['', Validators.required],
      registrationOpenDate: ['', Validators.required],
      registrationDeadline: ['', Validators.required],
      expectedStartDate: ['', Validators.required],
      estimatedEndDate: ['', Validators.required],
      status: ['open', Validators.required]
    });
  }

  ngOnInit(): void {
    this.fetchActiveAcademicYear();
    this.fetchLevels();
    this.fetchTrainingSessions();
    this.fetchInProgressSessions();
    this.initForm();
  }

  fetchActiveAcademicYear() {
    this.isLoadingYear.set(true);
    this.errorMessage.set(null);
    this.academicYearService.getActiveAcademicYear().subscribe({
      next: response => {
        if (response.success && response.data) {
          this.activeYear.set(response.data);
        } else {
          this.errorMessage.set("Aucune année scolaire active n'est configurée.");
        }
        this.isLoadingYear.set(false);
      },
      error: err => {
        console.error('Erreur API Année Active:', err);
        this.errorMessage.set("Aucune année scolaire active n'est configurée.");
        this.isLoadingYear.set(false);
      }
    });
  }

  fetchLevels() {
    this.isLoadingLevels.set(true);
    this.levelsError.set(null);
    this.levelService.getAllLevels().subscribe({
      next: response => {
        if (response.success && response.data) {
          this.levels.set(response.data);
        } else {
          this.levelsError.set('Impossible de charger les niveaux.');
        }
        this.isLoadingLevels.set(false);
      },
      error: err => {
        console.error('Erreur API Niveaux:', err);
        this.levelsError.set('Erreur de connexion.');
        this.isLoadingLevels.set(false);
      }
    });
  }

  onLevelChange(event: Event) {
    const selectElement = event.target as HTMLSelectElement;
    const levelId = Number(selectElement.value);
    this.selectedLevelId.set(levelId);
    this.selectedSpecialtyId.set(null);
    this.specialties.set([]);
    this.specialtiesError.set(null);
    if (levelId) {
      this.fetchSpecialtiesByLevel(levelId);
    }
  }

  onSpecialtyChange(event: Event) {
    const selectElement = event.target as HTMLSelectElement;
    this.selectedSpecialtyId.set(Number(selectElement.value));
  }

  fetchSpecialtiesByLevel(levelId: number) {
    this.isLoadingSpecialties.set(true);
    this.specialtyService.getSpecialtiesByLevel(levelId).subscribe({
      next: response => {
        if (response.success && response.data) {
          this.specialties.set(response.data);
          if (response.data.length === 0) {
            this.specialtiesError.set('Aucune spécialité pour ce niveau.');
          }
        } else {
          this.specialtiesError.set('Impossible de charger les spécialités.');
        }
        this.isLoadingSpecialties.set(false);
      },
      error: err => {
        console.error('Erreur API Spécialités:', err);
        this.specialtiesError.set('Erreur de connexion.');
        this.isLoadingSpecialties.set(false);
      }
    });
  }

  fetchTrainingSessions() {
    this.isLoadingSessions.set(true);
    this.trainingSessionService
      .getPaginatedTrainingSessions(this.currentPage(), this.pageSize())
      .subscribe({
        next: response => {
          if (response.success && response.data) {
            this.trainingSessions.set(response.data.content!);
            this.totalElements.set(response.data.totalElements || 0);
          }
          this.isLoadingSessions.set(false);
        },
        error: err => {
          console.error('Erreur de chargement des sessions:', err);
          this.isLoadingSessions.set(false);
        }
      });
  }

  fetchInProgressSessions() {
    this.isLoadingInProgress.set(true);
    this.trainingSessionService.getInProgressSessions().subscribe({
      next: response => {
        if (response.success && response.data) {
          this.inProgressSessions.set(response.data);
        }
        this.isLoadingInProgress.set(false);
      },
      error: err => {
        console.error('Erreur API Promotions en cours:', err);
        this.isLoadingInProgress.set(false);
      }
    });
  }

  // --- MODALS (ADD AND EDIT) ---
  openModal() {
    this.editingSession.set(null); // Creation mode
    this.promotionForm.reset({ status: 'open' });
    this.isModalOpen.set(true);
  }

  // Pre-fill the form for editing
  openEditModal(session: TrainingSessionResponse) {
    this.editingSession.set(session);

    // Fill the form with existing values
    this.promotionForm.patchValue({
      promotionName: session.promotionName,
      registrationOpenDate: session.registrationOpenDate,
      registrationDeadline: session.registrationDeadline,
      expectedStartDate: session.expectedStartDate,
      estimatedEndDate: session.estimatedEndDate,
      status: session.registrationsOpen ? 'open' : 'planned'
    });

    this.isModalOpen.set(true);
  }

  closeModal() {
    this.isModalOpen.set(false);
    this.editingSession.set(null);
    this.promotionForm.reset({ status: 'open' });
  }

  // Creation AND Editing Management
  savePromotion() {
    if (this.promotionForm.invalid) {
      this.promotionForm.markAllAsTouched();
      return;
    }

    const formValues = this.promotionForm.value;
    const editing = this.editingSession();

    // If editing, keep existing IDs. If creating, take the ones from filters.
    const academicYearId = editing ? editing.academicYearId : this.activeYear()?.id;
    const levelId = editing ? editing.levelId : this.selectedLevelId();
    const specialtyId = editing ? editing.specialtyId : this.selectedSpecialtyId();

    if (!academicYearId || !levelId || !specialtyId) {
      this.toastService.error('Contexte manquant (Année, Niveau ou Spécialité)');
      return;
    }

    const payload: TrainingSessionRequest = {
      promotionName: formValues.promotionName,
      academicYearId: academicYearId,
      levelId: levelId,
      specialtyId: specialtyId,
      expectedStartDate: formValues.expectedStartDate,
      estimatedEndDate: formValues.estimatedEndDate,
      registrationOpenDate: formValues.registrationOpenDate,
      registrationDeadline: formValues.registrationDeadline,
      status: TrainingSessionRequest.StatusEnum.InProgress, // Backend handles the IN_PROGRESS state by default
      registrationsOpen: formValues.status === 'open'
    };

    this.isSaving.set(true);

    if (editing) {
      // MODE: UPDATE
      this.trainingSessionService.updateTrainingSession(editing.id!, payload).subscribe({
        next: () => {
          this.toastService.success('Promotion modifiée avec succès !');
          this.handleSuccess();
        },
        error: err => this.handleError(err)
      });
    } else {
      // MODE: CREATION
      this.trainingSessionService.createTrainingSession(payload).subscribe({
        next: () => {
          this.toastService.success('Promotion ajoutée avec succès !');
          this.handleSuccess();
        },
        error: err => this.handleError(err)
      });
    }
  }

  // Utility functions for save success/error
  private handleSuccess() {
    this.isSaving.set(false);
    this.closeModal();
    this.fetchTrainingSessions();
    this.fetchInProgressSessions();
  }

  private handleError(err: any) {
    this.isSaving.set(false);
    if (err?.error?.errors) {
      err.error.errors.forEach((e: any) => {
        if (e.message === 'ESTIMATED END DATE MUST BE AFTER EXPECTED START DATE') {
          this.toastService.error('La date de fin doit être postérieure à la date de début.');
        } else if (e.message === 'REGISTRATION DEADLINE MUST BE AFTER REGISTRATION OPEN DATE') {
          this.toastService.error(
            "La date limite d'inscription doit être postérieure à l'ouverture."
          );
        } else if (e.message === 'PROMOTION_NAME_ALREADY_EXISTS') {
          this.toastService.error('Le nom de la promotion existe déjà.');
        } else if (e.message === 'TRAINING_SESSION_COMBINATION_ALREADY_EXISTS') {
          this.toastService.error('Cette combinaison de session de formation existe déjà.');
        }
      });
    } else {
      this.toastService.error("Une erreur inattendue s'est produite.");
    }
  }

  // --- DETAILS MODAL ---
  openDetailsModal(session: TrainingSessionResponse) {
    this.selectedSession.set(session);
  }

  closeDetailsModal() {
    this.selectedSession.set(null);
  }

  // --- PAGINATION ---
  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.fetchTrainingSessions();
  }

  // --- STATUS UTILITIES ---
  getStatusLabel(status: string | undefined): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'En cours';
      case 'FINISHED':
        return 'Terminée';
      default:
        return status || 'Inconnu';
    }
  }

  getStatusClasses(status: string | undefined): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'bg-green-50 text-green-700';
      case 'FINISHED':
        return 'bg-slate-100 text-slate-600';
      default:
        return 'bg-gray-50 text-gray-500';
    }
  }
}
