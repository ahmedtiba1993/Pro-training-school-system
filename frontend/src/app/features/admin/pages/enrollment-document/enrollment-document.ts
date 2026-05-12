import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';
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
  private readonly http = inject(HttpClient);

  // Signals (State)
  documents = signal<EnrollmentDocumentResponse[]>([]);
  levels = signal<LevelResponse[]>([]);

  isLoading = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  isExporting = signal<boolean>(false);
  isModalOpen = signal<boolean>(false);

  // Track currently edited document
  selectedDocId = signal<number | null>(null);

  // Computed
  isEditMode = computed(() => this.selectedDocId() !== null);
  totalDocuments = computed(() => this.documents().length);
  mandatoryDocuments = computed(() => this.documents().filter(doc => doc.mandatory).length);

  // Form
  documentForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.minLength(2)]],
    label: ['', [Validators.required, Validators.minLength(3)]],
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
    this.documentService
      .getAllDocuments()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          const data = response.data || response;
          this.documents.set(data);
        },
        error: () => this.toastService.error('Erreur lors du chargement des documents')
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

  exportArabicPdf(): void {
    this.isExporting.set(true);

    this.documentService
      .exportArabicForm()
      .pipe(finalize(() => this.isExporting.set(false)))
      .subscribe({
        next: (response: any) => {
          const blob = new Blob([response], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);

          const link = document.createElement('a');
          link.href = url;
          link.download = 'fiche_inscription.pdf';

          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);

          window.URL.revokeObjectURL(url);
          this.toastService.success('Fichier PDF téléchargé avec succès');
        },
        error: err => {
          console.error('Erreur Export PDF:', err);
        }
      });
  }

  openModal(doc?: EnrollmentDocumentResponse): void {
    if (doc) {
      // EDIT MODE
      this.selectedDocId.set(doc.id!);
      const extractedLevelIds = doc.levels ? doc.levels.map(l => l.id) : [];

      this.documentForm.patchValue({
        code: doc.code,
        label: doc.label,
        quantity: doc.quantity,
        nature: doc.nature,
        condition: doc.condition,
        levelIds: extractedLevelIds,
        mandatory: doc.mandatory
      });
      this.documentForm.get('code')?.disable();
    } else {
      // CREATE MODE
      this.selectedDocId.set(null);
      // Enable the field for a new document
      this.documentForm.get('code')?.enable();

      this.documentForm.reset({
        code: '',
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

  onSubmit(): void {
    if (this.documentForm.invalid) {
      this.documentForm.markAllAsTouched();
      this.toastService.error('Veuillez remplir correctement tous les champs obligatoires');
      return;
    }

    this.isSubmitting.set(true);
    // Use getRawValue() to ensure the `code` is retrieved
    // even if it is disabled (disable() ignores values with .value).
    const request: EnrollmentDocumentRequest = { ...this.documentForm.getRawValue() };

    if (this.isEditMode()) {
      this.documentService
        .updateDocument(this.selectedDocId()!, request)
        .pipe(finalize(() => this.isSubmitting.set(false)))
        .subscribe({
          next: () => {
            this.toastService.success('Document mis à jour avec succès');
            this.closeModal();
            this.loadDocuments();
          },
          error: err => {
            console.error(err);
            this.toastService.error('Erreur lors de la mise à jour');
          }
        });
    } else {
      this.documentService
        .createDocument(request)
        .pipe(finalize(() => this.isSubmitting.set(false)))
        .subscribe({
          next: () => {
            this.toastService.success('Document ajouté avec succès');
            this.closeModal();
            this.loadDocuments();
          },
          error: err => {
            console.error(err);
            this.toastService.error("Erreur lors de l'enregistrement");
          }
        });
    }
  }

  // Level checkboxes
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
