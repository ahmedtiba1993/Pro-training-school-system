import { Component, inject, OnInit, signal } from '@angular/core';
import { LevelControllerService } from '../../../../../core/api';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';

export interface LevelDto {
  id: number;
  code: string;
  label: string;
  accessLevel?: string;
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

  // Signal to store the ID of the level being edited (null if adding)
  editingLevelId = signal<number | null>(null);

  // Access level translation dictionary
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

  // --- OPEN MODAL FOR ADDING ---
  openAddModal(): void {
    this.editingLevelId.set(null); // On s'assure qu'aucun ID n'est stocké
    this.levelForm.reset({ accessLevel: '' });
    this.levelForm.markAsUntouched();
    this.isModalOpen.set(true);
  }

  // --- OPEN MODAL FOR EDITING---
  openEditModal(level: LevelDto): void {
    this.editingLevelId.set(level.id);

    // Fill form with existing data
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
    this.editingLevelId.set(null); // Reset ID on close
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
      // UPDATE LOGIC
      this.levelService.updateLevel(currentId, request).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Niveau modifié avec succès');
            this.closeModal();
            this.loadLevels();
          } else {
            this.toastService.error(res.message || 'Erreur lors de la modification');
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          this.toastService.error('Erreur lors de la modification du niveau');
          this.isSubmitting.set(false);
        }
      });
    } else {
      // ADD LOGIC
      this.levelService.createLevel(request).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Niveau ajouté avec succès');
            this.closeModal();
            this.loadLevels();
          } else {
            this.toastService.error(res.message || 'Erreur lors de la création');
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          this.toastService.error('Erreur lors de la création du niveau');
          this.isSubmitting.set(false);
        }
      });
    }
  }

  //Method to get translated label in HTML
  getAccessLevelLabel(code: string | undefined): string {
    if (!code) return 'Non défini';
    // Returns translation if it exists, otherwise displays raw code
    return this.accessLevelTranslations[code] || code;
  }
}
