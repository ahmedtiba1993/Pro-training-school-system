import { Component, inject, OnInit, signal } from '@angular/core';
import { LevelControllerService } from '../../../../../core/api';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';

export interface LevelDto {
  id: number;
  code: string;
  label: string;
  accessLevel?: string;
  isActive: boolean;
}

@Component({
  selector: 'app-level',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './level.html',
  styleUrl: './level.css'
})
export class Level implements OnInit {
  private levelService = inject(LevelControllerService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);

  levels = signal<LevelDto[]>([]);
  isLoading = signal<boolean>(true);
  isModalOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  // --- Signals for the status confirmation modal ---
  isConfirmModalOpen = signal<boolean>(false);
  levelToToggle = signal<LevelDto | null>(null);
  isToggling = signal<boolean>(false);

  editingLevelId = signal<number | null>(null);

  private accessLevelTranslations: Record<string, string> = {
    GRADE_9: '9ème Année de Base accomplie ou équivalant',
    SECONDARY_1: '1ère Année Secondaire accomplie ou équivalent',
    SECONDARY_2: '2ème Année Secondaire accomplie ou équivalent',
    SECONDARY_3: '3ème Année Secondaire accomplie ou équivalent',
    BACCALAUREATE: 'Baccalauréat accomplie ou équivalent',
    BACHELORS_DEGREE: 'Licence ou équivalent',
    NONE: 'Accès libre (Sans condition)'
  };

  levelForm: FormGroup = this.fb.group({
    code: ['', [Validators.required]],
    label: ['', [Validators.required]],
    accessLevel: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadLevels();
  }

  loadLevels(): void {
    this.isLoading.set(true);
    this.levelService.getAllLevels().subscribe({
      next: (response: any) => {
        if (response.success) {
          this.levels.set(response.data);
        } else {
          this.toastService.error(response.message || 'Erreur lors du chargement.');
        }
        this.isLoading.set(false);
      },
      error: err => {
        this.toastService.error('Erreur de connexion au serveur');
        this.isLoading.set(false);
      }
    });
  }

  // --- OPEN THE CONFIRMATION MODAL ---
  openToggleConfirmModal(level: LevelDto): void {
    this.levelToToggle.set(level);
    this.isConfirmModalOpen.set(true);
  }

  // --- CLOSE THE CONFIRMATION MODAL ---
  closeConfirmModal(): void {
    this.isConfirmModalOpen.set(false);
    this.levelToToggle.set(null);
  }

  // --- CONFIRM AND CALL THE API ---
  confirmToggleLevelStatus(): void {
    const level = this.levelToToggle();
    if (!level) return;

    this.isToggling.set(true);
    const newStatus = !level.isActive;

    this.levelService.updateLevelStatus(level.id, newStatus).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success(`Niveau ${newStatus ? 'activé' : 'désactivé'} avec succès`);
          this.levels.update(currentLevels =>
            currentLevels.map(l => (l.id === level.id ? { ...l, isActive: newStatus } : l))
          );
          this.closeConfirmModal();
        } else {
          this.toastService.error(res.message || 'Erreur lors du changement de statut');
        }
        this.isToggling.set(false);
      },
      error: err => {
        this.toastService.error('Erreur de connexion au serveur');
        this.isToggling.set(false);
      }
    });
  }

  openAddModal(): void {
    this.editingLevelId.set(null);
    this.levelForm.reset({ accessLevel: '' });
    this.levelForm.markAsUntouched();
    this.isModalOpen.set(true);
  }

  openEditModal(level: LevelDto): void {
    this.editingLevelId.set(level.id);
    this.levelForm.patchValue({
      code: level.code,
      label: level.label,
      accessLevel: level.accessLevel || ''
    });
    this.levelForm.markAsUntouched();
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.editingLevelId.set(null);
  }

  private handleBackendErrors(payload: any): void {
    const errorMap: Record<string, string> = {
      SPECIALTY_LEVEL_CODE_ALREADY_EXISTS: 'Ce code existe déjà.',
      CODE_ALREADY_EXISTS: 'Ce code existe déjà.',
      SPECIALTY_LEVEL_LABEL_ALREADY_EXISTS: 'Ce nom existe déjà.',
      LABEL_ALREADY_EXISTS: 'Ce nom existe déjà.'
    };

    if (payload && payload.errors && Array.isArray(payload.errors)) {
      payload.errors.forEach((err: any) => {
        const msg = errorMap[err.message] || err.message || 'Une erreur est survenue.';
        this.toastService.error(msg);
      });
    } else if (payload?.message && errorMap[payload.message]) {
      this.toastService.error(errorMap[payload.message]);
    } else {
      this.toastService.error(payload?.message || 'Erreur lors de la sauvegarde.');
    }
  }

  onSubmit(): void {
    if (this.levelForm.invalid) {
      this.levelForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const request = this.levelForm.value;
    const currentId = this.editingLevelId();

    if (currentId !== null) {
      this.levelService.updateLevel(currentId, request).subscribe({
        next: (res: any) => {
          if (res.success) {
            const successMessages: Record<string, string> = {
              SPECIALTY_LEVEL_UPDATED: 'Niveau modifié avec succès',
              LEVEL_UPDATED_SUCCESSFULLY: 'Niveau modifié avec succès'
            };
            const toastMsg = (res.message && successMessages[res.message]) || 'Niveau modifié avec succès';
            this.toastService.success(toastMsg);
            this.closeModal();
            this.loadLevels();
          } else {
            this.handleBackendErrors(res);
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          this.handleBackendErrors(err.error);
          this.isSubmitting.set(false);
        }
      });
    } else {
      this.levelService.createLevel(request).subscribe({
        next: (res: any) => {
          if (res.success) {
            const successMessages: Record<string, string> = {
              SPECIALTY_LEVEL_CREATED: 'Niveau ajouté avec succès',
              LEVEL_CREATED_SUCCESSFULLY: 'Niveau ajouté avec succès'
            };
            const toastMsg = (res.message && successMessages[res.message]) || 'Niveau ajouté avec succès';
            this.toastService.success(toastMsg);
            this.closeModal();
            this.loadLevels();
          } else {
            this.handleBackendErrors(res);
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          this.handleBackendErrors(err.error);
          this.isSubmitting.set(false);
        }
      });
    }
  }

  getAccessLevelLabel(code: string | undefined): string {
    if (!code) return 'Non défini';
    return this.accessLevelTranslations[code] || code;
  }
}
