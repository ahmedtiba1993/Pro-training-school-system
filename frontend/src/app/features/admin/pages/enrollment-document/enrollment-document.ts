import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
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
  // Service and utility injections
  private readonly documentService = inject(EnrollmentDocumentControllerService);
  private readonly levelService = inject(LevelControllerService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  // Reactive signals (component state)
  documents = signal<EnrollmentDocumentResponse[]>([]);
  levels = signal<LevelResponse[]>([]);

  isLoading = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  isExporting = signal<boolean>(false);
  isExportDropdownOpen = signal<boolean>(false);
  isModalOpen = signal<boolean>(false);

  // Track currently edited document
  selectedDocId = signal<number | null>(null);

  // Computed state
  isEditMode = computed(() => this.selectedDocId() !== null);
  totalDocuments = computed(() => this.documents().length);
  mandatoryDocuments = computed(() => this.documents().filter(doc => doc.mandatory).length);
  activeDocuments = computed(() => this.documents().filter(doc => doc.isActive).length);

  // Status toggle in-progress tracking
  togglingDocIds = signal<Set<number>>(new Set());

  // Reactive form
  documentForm: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.minLength(2)]],
    label: ['', [Validators.required, Validators.minLength(3)]],
    labelAr: ['', [Validators.required, Validators.minLength(3)]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    nature: ['ORIGINAL', Validators.required],
    condition: ['ALL_REGISTRATIONS', Validators.required],
    levelIds: [[], [Validators.required]],
    mandatory: [true, Validators.required]
  });

  // UI label dictionaries
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

  readonly errorMessages: Record<string, string> = {
    DOCUMENTS_ENROLLMENT_DOCUMENT_LABEL_ALREADY_EXISTS: 'Un document avec ce libellé existe déjà.',
    DOCUMENT_LABEL_ALREADY_EXISTS: 'Un document avec ce libellé existe déjà.',
    DOCUMENTS_ENROLLMENT_DOCUMENT_CODE_ALREADY_EXISTS: 'Un document avec ce code existe déjà.',
    DOCUMENTS_ENROLLMENT_DOCUMENT_NOT_FOUND: 'Le document demandé est introuvable.',
    DOCUMENT_NOT_FOUND: 'Le document demandé est introuvable.'
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
    this.levelService.getActiveLevels().subscribe({
      next: (response: any) => {
        const data = response.data || response;
        this.levels.set(data);
      },
      error: () => this.toastService.error('Impossible de charger les niveaux')
    });
  }

  toggleExportDropdown(): void {
    this.isExportDropdownOpen.update(open => !open);
  }

  exportArabicPdf(level?: LevelResponse): void {
    this.isExporting.set(true);
    this.isExportDropdownOpen.set(false);

    const levelId = level?.id;

    this.documentService
      .exportArabicForm(levelId, 'body', false, {
        httpHeaderAccept: 'application/pdf' as any
      })
      .pipe(finalize(() => this.isExporting.set(false)))
      .subscribe({
        next: (response: any) => {
          const blob =
            response instanceof Blob
              ? response
              : new Blob([response], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);

          const now = new Date();
          const timestamp = now
            .toISOString()
            .replace(/T/, '_')
            .replace(/:/g, '-')
            .slice(0, 19);

          const levelSuffix = level?.code ? `_${level.code}` : '_tous_niveaux';
          const link = document.createElement('a');
          link.href = url;
          link.download = `fiche_inscription${levelSuffix}_${timestamp}.pdf`;

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
        labelAr: doc.labelAr || '',
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
        label: '',
        labelAr: '',
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
          error: (err: HttpErrorResponse) => {
            this.handleBackendError(err, 'Erreur lors de la mise à jour');
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
          error: (err: HttpErrorResponse) => {
            this.handleBackendError(err, "Erreur lors de l'enregistrement");
          }
        });
    }
  }

  /**
   * Centralizes and maps backend technical error messages to user-friendly notifications.
   */
  handleBackendError(err: HttpErrorResponse | any, fallbackMessage: string): void {
    const errorBody = err?.error;
    const errorCode = errorBody?.message || errorBody?.errorCode;

    if (errorBody?.errors && Array.isArray(errorBody.errors) && errorBody.errors.length > 0) {
      errorBody.errors.forEach((e: { field?: string; message?: string }) => {
        const msg = (e.message && this.errorMessages[e.message]) || e.message || fallbackMessage;
        this.toastService.error(msg);
      });
      return;
    }

    if (errorCode && this.errorMessages[errorCode]) {
      this.toastService.error(this.errorMessages[errorCode]);
      return;
    }

    if (err?.status === 409) {
      this.toastService.error('Un conflit est survenu : ce document existe déjà.');
      return;
    }

    if (errorBody?.message) {
      this.toastService.error(errorBody.message);
      return;
    }

    this.toastService.error(fallbackMessage);
  }

  // Level selection handlers
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

  isToggling(docId?: number): boolean {
    return docId !== undefined && docId !== null ? this.togglingDocIds().has(docId) : false;
  }

  toggleDocumentStatus(doc: EnrollmentDocumentResponse): void {
    if (!doc.id || this.isToggling(doc.id)) {
      return;
    }

    const docId = doc.id;
    const newStatus = !doc.isActive;

    this.togglingDocIds.update(set => new Set(set).add(docId));

    this.documentService
      .updateDocumentStatus(docId, newStatus)
      .pipe(
        finalize(() => {
          this.togglingDocIds.update(set => {
            const next = new Set(set);
            next.delete(docId);
            return next;
          });
        })
      )
      .subscribe({
        next: () => {
          this.toastService.success(`Document ${newStatus ? 'activé' : 'désactivé'} avec succès`);
          this.documents.update(docs =>
            docs.map(d => (d.id === docId ? { ...d, isActive: newStatus } : d))
          );
        },
        error: (err: HttpErrorResponse) => {
          this.handleBackendError(err, 'Erreur lors du changement de statut');
        }
      });
  }
}
