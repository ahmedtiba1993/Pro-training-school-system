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

  // signal pour modal
  showModal = signal(false);

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

  //Model Add
  openModal() {
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.levelForm.reset();
  }

  addLevel() {
    if (this.levelForm.invalid) {
      // Force showing all validation errors
      this.levelForm.markAllAsTouched();
      return;
    }
    console.log(this.isSaving);
    this.isSaving.set(true);
    console.log(this.isSaving);

    if (this.levelForm.invalid) {
      this.levelForm.markAllAsTouched();
      return;
    }

    const payload = this.levelForm.value;

    this.levelService.createLevel(payload).subscribe({
      next: res => {
        this.isSaving.set(false);

        this.fetchLevels();
        this.closeModal();
        this.toastService.success('ajoutée avec succès');
      },
      error: err => {
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
        this.isSaving.set(false);
        console.error('Erreur lors de l’ajout du niveau', err);
      }
    });
  }
}
