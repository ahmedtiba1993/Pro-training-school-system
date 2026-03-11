import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  LevelControllerService,
  LevelDto,
  SpecialtyControllerService,
  SpecialtyRequest,
  SpecialtyResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-specialty',
  imports: [ReactiveFormsModule],
  templateUrl: './specialty.html',
  styleUrl: './specialty.css'
})
export class Specialty {
  private specialtyService = inject(SpecialtyControllerService);
  private levelService = inject(LevelControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  // Data Signals
  specialties = signal<SpecialtyResponse[]>([]);
  levels = signal<LevelDto[]>([]);

  // State Signals
  isLoading = signal<boolean>(true);
  isSaving = signal<boolean>(false);

  // Modal Signals
  showModal = signal(false);
  editingSpecialtyId = signal<number | null>(null);

  // FormGroup
  specialtyForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    code: ['', Validators.required],
    levelIds: [[], Validators.required] // Initialisé avec un tableau vide
  });

  ngOnInit(): void {
    this.fetchSpecialties();
    this.fetchLevels();
  }

  fetchSpecialties(): void {
    this.isLoading.set(true);
    this.specialtyService.getAllSpecialties().subscribe({
      next: response => {
        this.specialties.set(response.data || []);
        this.isLoading.set(false);
      },
      error: error => {
        console.error('Erreur lors de la récupération des spécialités', error);
        this.isLoading.set(false);
      }
    });
  }

  fetchLevels(): void {
    this.levelService.getAllLevels().subscribe({
      next: response => {
        this.levels.set(response.data || []);
      },
      error: error => console.error('Erreur lors de la récupération des niveaux', error)
    });
  }

  // ---- Modal Management (Add & Update) ----
  // ---- Modal Management (Add & Update) ----
  openAddModal() {
    this.editingSpecialtyId.set(null);
    this.specialtyForm.reset({ levelIds: [] }); // S'assure que le formulaire est vide
    this.showModal.set(true);
  }

  openEditModal(specialty: SpecialtyResponse) {
    if (specialty && specialty.id != null) {
      this.editingSpecialtyId.set(specialty.id);

      // 1. On dit à TypeScript : "Fais-moi confiance, à l'exécution c'est un vrai tableau"
      const levelsArray = ((specialty.associatedLevels as unknown) as LevelDto[]) || [];

      // 2. On peut maintenant utiliser le .map() en toute sécurité
      const associatedLevelIds = levelsArray.map(l => l.id);

      this.specialtyForm.patchValue({
        name: specialty.name,
        code: specialty.code,
        levelIds: associatedLevelIds
      });

      this.showModal.set(true);
    }
  }

  closeModal() {
    this.showModal.set(false);
    this.specialtyForm.reset({ levelIds: [] });
    this.editingSpecialtyId.set(null);
  }

  // ---- Checkbox Management (Levels) ----
  toggleLevel(levelId: number, event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    const currentLevelIds = (this.specialtyForm.get('levelIds')?.value as number[]) || [];

    if (isChecked) {
      // If checked , add to table
      this.specialtyForm.patchValue({ levelIds: [...currentLevelIds, levelId] });
    } else {
      // If unchecked, remove the level from the list
      this.specialtyForm.patchValue({ levelIds: currentLevelIds.filter(id => id !== levelId) });
    }
  }

  isLevelSelected(levelId: number): boolean {
    const currentLevelIds = (this.specialtyForm.get('levelIds')?.value as number[]) || [];
    return currentLevelIds.includes(levelId);
  }

  // ---- Save Logic ----
  saveSpecialty() {
    // 1. Vérifier si le formulaire est valide
    if (this.specialtyForm.invalid) {
      this.specialtyForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const payload: SpecialtyRequest = this.specialtyForm.value;
    const currentId = this.editingSpecialtyId();

    // Prepare Request (Update if ID exists, else Create)
    let request$;

    if (currentId) {
      // Si on a un ID, on fait une modification (PUT)
      request$ = this.specialtyService.updateSpecialty(currentId, payload);
    } else {
      // Sinon, on fait une création (POST)
      request$ = this.specialtyService.createSpecialty(payload);
    }

    request$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.fetchSpecialties();
        this.closeModal();

        const msg = currentId ? 'Spécialité modifiée' : 'Spécialité ajoutée';
        this.toastService.success(msg);
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
              case 'name':
                if (e.message === 'SPECIALTY_NAME_ALREADY_EXISTS') {
                  this.toastService.error('Une spécialité avec ce nom existe déjà');
                }
                break;
            }
          });
        }
      }
    });
  }
}
