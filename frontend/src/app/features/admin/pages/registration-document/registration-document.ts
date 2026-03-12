import { Component, computed, inject, signal } from '@angular/core';
import {
  LevelControllerService,
  LevelDto,
  RegistrationDocumentControllerService,
  RegistrationDocumentResponse
} from '../../../../core/api';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../shared/services/toast.service';

@Component({
  selector: 'app-registration-document',
  imports: [ReactiveFormsModule],
  templateUrl: './registration-document.html',
  styleUrl: './registration-document.css'
})
export class RegistrationDocument {
  // Inject the Swagger-generated service
  private documentService = inject(RegistrationDocumentControllerService);
  private levelService = inject(LevelControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  // Signals to manage the UI state
  documents = signal<RegistrationDocumentResponse[]>([]);
  isLoading = signal<boolean>(true);
  levels = signal<LevelDto[]>([]);

  // Compute the total number of documents
  totalDocuments = computed(() => this.documents().length);

  // Signal for modal state
  isModalOpen = signal<boolean>(false);

  //loading
  isSaving = signal<boolean>(false);

  // Compute the number of documents that have "isMandatory" set to true
  mandatoryDocumentsCount = computed(() => this.documents().filter(doc => doc.mandatory).length);

  //Form
  documentForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    nature: ['ORIGINAL', Validators.required],
    condition: ['ALL_REGISTRATIONS', Validators.required],
    levelIds: [[], Validators.required],
    mandatory: [true]
  });

  ngOnInit(): void {
    this.fetchDocuments();
    this.fetchLevels();
  }

  fetchDocuments(): void {
    this.isLoading.set(true);

    this.documentService.getAllDocuments().subscribe({
      next: response => {
        this.documents.set(response.data || []);
        this.isLoading.set(false);
      },
      error: error => {
        console.error('Error while fetching documents', error);
        this.isLoading.set(false);
      }
    });
  }

  fetchLevels(): void {
    this.levelService.getAllLevels().subscribe({
      next: response => {
        // Assuming your ApiResponse returns the list in "response.data"
        this.levels.set(response.data || []);
      },
      error: error => console.error('Error while fetching levels', error)
    });
  }

  // Modal controls
  openModal() {
    this.documentForm.reset({
      quantity: 1,
      nature: 'ORIGINAL',
      condition: 'ALL_REGISTRATIONS',
      levelIds: [],
      mandatory: true
    });
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  // Handles selection/deselection of level checkboxes
  onLevelChange(event: any, levelId: number) {
    const isChecked = event.target.checked;
    const currentLevelIds = this.documentForm.get('levelIds')?.value as number[];

    if (isChecked) {
      // Add the level ID to the array
      this.documentForm.patchValue({ levelIds: [...currentLevelIds, levelId] });
    } else {
      // Remove the level ID from the array
      this.documentForm.patchValue({ levelIds: currentLevelIds.filter(id => id !== levelId) });
    }
  }

  saveDocument() {
    // Check if the form is valid
    if (this.documentForm.invalid) {
      this.documentForm.markAllAsTouched();
      return;
    }

    // Enable loading state
    this.isSaving.set(true);

    // Retrieve form data
    const payload = this.documentForm.value;

    // Call the API to create the document
    this.documentService.createDocument(payload).subscribe({
      next: response => {
        // Succès
        this.isSaving.set(false);
        this.closeModal();
        this.fetchDocuments();

        this.toastService.success('Document ajouté avec succès');
      },
      error: err => {
        // Erreur
        this.isSaving.set(false);
        const errorMessage = err?.error?.message || err?.error?.errors?.[0]?.message;

        if (errorMessage === 'DOCUMENT_NAME_ALREADY_EXISTS') {
          this.documentForm.get('name')?.setErrors({ alreadyExists: true });
          this.toastService.error('Ce nom de document existe déjà.');
        } else {
          this.toastService.error('Une erreur est survenue.');
        }
      }
    });
  }
}
