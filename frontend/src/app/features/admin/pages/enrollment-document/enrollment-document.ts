import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  EnrollmentDocumentControllerService,
  LevelControllerService,
  EnrollmentDocumentResponse,
  EnrollmentDocumentRequest,
  LevelResponse
} from '../../../../core/api';
import { ToastService } from '../../../../shared/services/toast.service';

@Component({
  selector: 'app-enrollment-document',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './enrollment-document.html',
  styleUrl: './enrollment-document.css'
})
export class EnrollmentDocumentComponent implements OnInit {
  // Injections
  private readonly documentService = inject(EnrollmentDocumentControllerService);
  private readonly levelService = inject(LevelControllerService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  // Signals (State)
  documents = signal<EnrollmentDocumentResponse[]>([]);
  levels = signal<LevelResponse[]>([]);

  isLoading = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  isModalOpen = signal<boolean>(false);

  // Track currently edited document
  selectedDocId = signal<number | null>(null);

  // Computed to determine if in edit mode
  isEditMode = computed(() => this.selectedDocId() !== null);

  // Computed
  totalDocuments = computed(() => this.documents().length);
  mandatoryDocuments = computed(() => this.documents().filter(doc => doc.mandatory).length);

  // Form
  documentForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    nature: ['ORIGINAL', Validators.required],
    condition: ['ALL_REGISTRATIONS', Validators.required],
    levelIds: [[], [Validators.required]],
    mandatory: [true, Validators.required]
  });

  // Dictionaries
  readonly natureLabels: Record<string, string> = {
    ORIGINAL: 'Original',
    COPY: 'Copie simple',
    CERTIFIED_COPY: 'Copie conforme',
    PHYSICAL: 'Format physique'
  };

  readonly conditionLabels: Record<string, string> = {
    ALL_REGISTRATIONS: 'Général (Tous)',
    FOREIGN_STUDENT: 'Étudiants Étrangers',
    ADULT_ONLY: 'Majeurs uniquement'
  };

  ngOnInit(): void {
    this.loadDocuments();
    this.loadLevels();
  }

  loadDocuments(): void {
    this.isLoading.set(true);
    this.documentService.getAllDocuments().subscribe({
      next: (response: any) => {
        const data = response.data || response;
        this.documents.set(data);
      },
      error: () => this.toastService.error('Erreur lors du chargement des documents'),
      complete: () => this.isLoading.set(false)
    });
  }

  loadLevels(): void {
    this.levelService.getAllLevels().subscribe({
      next: (response: any) => {
        const data = response.data || response;
        this.levels.set(data);
      },
      error: () => this.toastService.error('Impossible de charger les niveaux')
    });
  }

  // Gère la création ET la modification
  openModal(doc?: EnrollmentDocumentResponse): void {
    if (doc) {
      // EDIT MODE: Store the ID and pre-fill the form
      this.selectedDocId.set(doc.id!);

      // Need to extract level IDs from the list of objects returned by the API
      const extractedLevelIds = doc.levels ? doc.levels.map(l => l.id) : [];

      this.documentForm.patchValue({
        name: doc.name,
        quantity: doc.quantity,
        nature: doc.nature,
        condition: doc.condition,
        levelIds: extractedLevelIds,
        mandatory: doc.mandatory
      });
    } else {
      // CREATE MODE: Reset to empty
      this.selectedDocId.set(null);
      this.documentForm.reset({
        quantity: 1,
        nature: 'ORIGINAL',
        condition: 'ALL_REGISTRATIONS',
        levelIds: [],
        mandatory: true
      });
    }
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.selectedDocId.set(null);
  }

  // Handle Create or Update call
  onSubmit(): void {
    if (this.documentForm.invalid) {
      this.documentForm.markAllAsTouched();
      this.toastService.error('Veuillez remplir correctement tous les champs obligatoires');
      return;
    }

    this.isSubmitting.set(true);
    const request: EnrollmentDocumentRequest = { ...this.documentForm.value };

    if (this.isEditMode()) {
      // API UPDATE
      this.documentService.updateDocument(this.selectedDocId()!, request).subscribe({
        next: () => {
          this.toastService.success('Document mis à jour avec succès');
          this.closeModal();
          this.loadDocuments();
        },
        error: err => {
          console.error(err);
          this.toastService.error('Erreur lors de la mise à jour');
        },
        complete: () => this.isSubmitting.set(false)
      });
    } else {
      // API CREATE
      this.documentService.createDocument(request).subscribe({
        next: () => {
          this.toastService.success('Document ajouté avec succès');
          this.closeModal();
          this.loadDocuments();
        },
        error: err => {
          console.error(err);
          this.toastService.error("Erreur lors de l'enregistrement");
        },
        complete: () => this.isSubmitting.set(false)
      });
    }
  }

  // --- Checkbox management ---
  onLevelChange(event: Event, levelId: number): void {
    const checkbox = event.target as HTMLInputElement;
    const currentLevelIds = this.documentForm.get('levelIds')?.value as number[];
    if (checkbox.checked) {
      this.documentForm.patchValue({ levelIds: [...currentLevelIds, levelId] });
    } else {
      this.documentForm.patchValue({ levelIds: currentLevelIds.filter(id => id !== levelId) });
    }
    this.documentForm.get('levelIds')?.markAsTouched();
  }

  isLevelSelected(levelId: number): boolean {
    const currentLevelIds = this.documentForm.get('levelIds')?.value as number[];
    return currentLevelIds ? currentLevelIds.includes(levelId) : false;
  }
}
