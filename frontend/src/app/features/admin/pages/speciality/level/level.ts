import { Component, inject, OnInit, signal } from '@angular/core';
import { LevelControllerService, LevelDto } from '../../../../../core/api';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-level',
  imports: [ReactiveFormsModule],
  templateUrl: './level.html',
  styleUrl: './level.css'
})
export class Level implements OnInit {
  private levelService = inject(LevelControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  levels = signal<LevelDto[]>([]);
  isLoading = signal<boolean>(true);
  isSaving = signal<boolean>(false);

  // Modal signals
  showModal = signal(false);
  editingLevelId = signal<number | null>(null);

  // FormGroup
  levelForm: FormGroup = this.fb.group({
    code: ['', Validators.required],
    label: ['', Validators.required],
    type: ['', Validators.required]
  });

  ngOnInit(): void {
    this.fetchLevels();
  }

  fetchLevels(): void {
    this.isLoading.set(true);

    this.levelService.getAllLevels().subscribe({
      next: response => {
        this.levels.set(response.data || []);
        this.isLoading.set(false);
      },
      error: error => {
        console.error('Erreur lors de la récupération des niveaux', error);
        this.isLoading.set(false);
      }
    });
  }

  getShortCode(code: string): string {
    if (!code) return '';
    return code.substring(0, 3).toUpperCase();
  }

  //Model Add & update
  openAddModal(id?: number) {
    if (id != null) {
      this.editingLevelId.set(id);
    }
    this.showModal.set(true);
  }

  openEditModal(level: LevelDto) {
    if (level && level.id != null) {
      this.editingLevelId.set(level.id);

      this.levelForm.patchValue({
        code: level.code,
        label: level.label,
        type: level.type
      });

      this.showModal.set(true);
    }
  }

  closeModal() {
    this.showModal.set(false);
    this.levelForm.reset();
    this.editingLevelId.set(null);
  }

  saveLevel() {
    if (this.levelForm.invalid) {
      this.levelForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const payload = this.levelForm.value;
    const currentId = this.editingLevelId();

    let request$;
    if (currentId) {
      request$ = this.levelService.updateLevel(currentId, payload);
    } else {
      request$ = this.levelService.createLevel(payload);
    }

    request$.subscribe({
      next: res => {
        this.isSaving.set(false);
        this.fetchLevels();
        this.closeModal();

        const successMessage = currentId
          ? 'Niveau modifié avec succès'
          : 'Niveau ajouté avec succès';
        this.toastService.success(successMessage);
      },
      error: err => {
        this.isSaving.set(false);

        if (err?.error?.errors) {
          err.error.errors.forEach((e: any) => {
            switch (e.field) {
              case 'code':
                if (e.message === 'CODE_ALREADY_EXISTS') {
                  this.toastService.error('Ce code existe déjà');
                }
                break;
            }
          });
        }

        const errorLogMessage = currentId
          ? 'Erreur lors de la modification'
          : 'Erreur lors de l’ajout';
        console.error(errorLogMessage, err);
      }
    });
  }
}
