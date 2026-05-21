import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
  ViewChild,
  ElementRef
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ReactiveFormsModule, NonNullableFormBuilder, Validators} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs/operators';
import {HttpErrorResponse} from '@angular/common/http';

import {SubjectControllerService} from '../../../../../core/api/api/subject-controller.service';
import {LevelControllerService} from '../../../../../core/api/api/level-controller.service';
import {TrainingControllerService} from '../../../../../core/api/api/training-controller.service';
import {SubjectResponse} from '../../../../../core/api/model/subject-response';
import {LevelResponse} from '../../../../../core/api/model/level-response';
import {TrainingResponse} from '../../../../../core/api/model/training-response';
import {SubjectRequest} from '../../../../../core/api/model/subject-request';
import {ToastService} from '../../../../../shared/services/toast.service';

interface ConfirmModalState {
  isOpen: boolean;
  subjectId: number | null;
  newStatus: 'ACTIVE' | 'ARCHIVED' | null;
}

interface PdfActionState {
  id: number;
  type: 'uploading' | 'downloading';
}

@Component({
  selector: 'app-subject-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './subject-list.html',
  styleUrl: './subject-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubjectList implements OnInit {
  private readonly subjectApiService = inject(SubjectControllerService);
  private readonly levelApiService = inject(LevelControllerService);
  private readonly trainingApiService = inject(TrainingControllerService);
  private readonly fb = inject(NonNullableFormBuilder);

  // TOAST SERVICE INJECTION
  private readonly toastService = inject(ToastService);

  // --- ViewChild element for the hidden file input ---
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  // --- State Signals ---
  public readonly subjects = signal<SubjectResponse[]>([]);
  public readonly isLoading = signal<boolean>(false);

  // --- Status Confirmation Modal State ---
  public readonly confirmModalState = signal<ConfirmModalState>({
    isOpen: false,
    subjectId: null,
    newStatus: null
  });
  public readonly isUpdatingStatus = signal<boolean>(false);

  // --- PDF Actions State ---
  public readonly pdfActionState = signal<PdfActionState | null>(null);
  private readonly pendingUploadSubjectId = signal<number | null>(null);

  // --- Filters State ---
  public readonly searchTerm = signal<string>('');
  public readonly filterLevelId = signal<number>(0);
  public readonly filterSpecialtyId = signal<number>(0);
  public readonly filterTrainings = signal<TrainingResponse[]>([]);
  public readonly isFilterTrainingsLoading = signal<boolean>(false);

  // --- Modal State (Creation / Edition) ---
  public readonly isModalOpen = signal<boolean>(false);
  public readonly isSubmitting = signal<boolean>(false);
  public readonly modalErrorMessage = signal<string | null>(null);
  public readonly editingSubject = signal<SubjectResponse | null>(null);

  public readonly levels = signal<LevelResponse[]>([]);
  public readonly isLevelsLoading = signal<boolean>(false);
  public readonly availableTrainings = signal<TrainingResponse[]>([]);
  public readonly isTrainingsLoading = signal<boolean>(false);

  // --- Reactive Form with strict validations ---
  public readonly subjectForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    code: ['', [Validators.required]],
    levelId: [0, [Validators.required, Validators.min(1)]], // min(1) forces selection of a real level
    trainingId: [{value: 0, disabled: true}, [Validators.required, Validators.min(1)]],
    defaultCoefficient: [1, [Validators.required, Validators.min(0.5)]],
    theoryHours: [0, [Validators.required, Validators.min(1)]],
    practicalHours: [0, [Validators.required, Validators.min(1)]]
  });

  // --- Computed States ---
  public readonly totalCount = computed(() => this.subjects().length);
  public readonly isEditMode = computed(() => this.editingSubject() !== null);

  public readonly filteredSubjects = computed(() => {
    const query = this.searchTerm()
      .toLowerCase()
      .trim();
    const targetLevelId = this.filterLevelId();
    const targetSpecId = this.filterSpecialtyId();
    const currentFilterTrainings = this.filterTrainings();

    return this.subjects().filter(subject => {
      const matchesSearch =
        !query ||
        subject.name?.toLowerCase().includes(query) ||
        subject.code?.toLowerCase().includes(query);

      let matchesSpecialty = true;
      if (targetSpecId > 0) {
        matchesSpecialty = subject.trainingId === targetSpecId;
      } else if (targetLevelId > 0) {
        const validSpecIds = currentFilterTrainings.map(t => t.id);
        matchesSpecialty = subject.trainingId != null && validSpecIds.includes(subject.trainingId);
      }
      return matchesSearch && matchesSpecialty;
    });
  });

  constructor() {
    // Cascade management on level modification
    this.subjectForm.controls.levelId.valueChanges.pipe(takeUntilDestroyed()).subscribe(levelId => {
      if (!this.isEditMode()) {
        if (levelId && levelId > 0) {
          this.fetchTrainingsByLevel(levelId);
        } else {
          this.clearTrainingControl();
        }
      }
    });
  }

  ngOnInit(): void {
    this.fetchActiveLevels();
    this.fetchSubjectsFromApi();
  }

  private clearTrainingControl(): void {
    this.availableTrainings.set([]);
    this.subjectForm.controls.trainingId.setValue(0);
    this.subjectForm.controls.trainingId.disable();
  }

  // --- API Fetches ---
  public fetchSubjectsFromApi(): void {
    this.isLoading.set(true);
    this.subjectApiService
      .getAllSubjects()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (res: any) => this.subjects.set(res.data || res || []),
        error: () => this.toastService.error('Impossible de charger le catalogue des matières.')
      });
  }

  private fetchActiveLevels(): void {
    this.isLevelsLoading.set(true);
    this.levelApiService
      .getActiveLevels()
      .pipe(finalize(() => this.isLevelsLoading.set(false)))
      .subscribe({
        next: (res: any) => this.levels.set(res.data || res || []),
        error: err => console.error('Error loading levels:', err)
      });
  }

  private fetchTrainingsByLevel(levelId: number): void {
    this.isTrainingsLoading.set(true);
    this.subjectForm.controls.trainingId.disable();
    this.trainingApiService
      .getOngoingTrainingsByLevel(levelId)
      .pipe(
        finalize(() => {
          this.isTrainingsLoading.set(false);
          this.subjectForm.controls.trainingId.enable();
        })
      )
      .subscribe({
        next: (res: any) => {
          this.availableTrainings.set(res.data || res || []);
          this.subjectForm.controls.trainingId.setValue(0);
        },
        error: err => console.error('Error loading trainings:', err)
      });
  }

  // --- PDF File Management ---
  public triggerFileUpload(subjectId: number): void {
    this.pendingUploadSubjectId.set(subjectId);
    this.fileInput.nativeElement.click();
  }

  public onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    const subjectId = this.pendingUploadSubjectId();

    if (subjectId && file.type === 'application/pdf') {
      this.pdfActionState.set({id: subjectId, type: 'uploading'});
      this.subjectApiService
        .uploadSubjectPdf(subjectId, file as any)
        .pipe(
          finalize(() => {
            this.pdfActionState.set(null);
            this.pendingUploadSubjectId.set(null);
            input.value = '';
          })
        )
        .subscribe({
          next: () => this.fetchSubjectsFromApi(),
          error: () => this.toastService.error("Erreur lors de l'envoi du fichier PDF.")
        });
    } else {
      this.toastService.error('Veuillez sélectionner un fichier PDF valide.');
      input.value = '';
      this.pendingUploadSubjectId.set(null);
    }
  }

  public downloadPdf(subjectId: number): void {
    this.pdfActionState.set({id: subjectId, type: 'downloading'});
    this.subjectApiService
      .getSubjectPdf(subjectId)
      .pipe(finalize(() => this.pdfActionState.set(null)))
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          window.open(url, '_blank');
        },
        error: () => this.toastService.error("Erreur lors de l'ouverture du PDF.")
      });
  }

  // --- Status Changes ---
  public promptStatusChange(subjectId: number | undefined, newStatus: 'ACTIVE' | 'ARCHIVED'): void {
    if (subjectId) this.confirmModalState.set({isOpen: true, subjectId, newStatus});
  }

  public closeConfirmModal(): void {
    if (!this.isUpdatingStatus()) {
      this.confirmModalState.set({isOpen: false, subjectId: null, newStatus: null});
    }
  }

  public confirmStatusChange(): void {
    const state = this.confirmModalState();
    if (!state.subjectId || !state.newStatus) return;

    this.isUpdatingStatus.set(true);
    this.subjectApiService
      .changeSubjectStatus(state.subjectId, state.newStatus as any)
      .pipe(
        finalize(() => {
          this.isUpdatingStatus.set(false);
          this.closeConfirmModal();
        })
      )
      .subscribe({
        next: () => this.fetchSubjectsFromApi(),
        error: () => this.toastService.error('Impossible de changer le statut.')
      });
  }

  // --- UI Filters ---
  public onSearchChange(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  public onFilterSpecialtyChange(event: Event): void {
    this.filterSpecialtyId.set(Number((event.target as HTMLSelectElement).value));
  }

  public onFilterLevelChange(event: Event): void {
    const levelId = Number((event.target as HTMLSelectElement).value);
    this.filterLevelId.set(levelId);
    this.filterSpecialtyId.set(0);

    if (levelId > 0) {
      this.isFilterTrainingsLoading.set(true);
      this.trainingApiService
        .getOngoingTrainingsByLevel(levelId)
        .pipe(finalize(() => this.isFilterTrainingsLoading.set(false)))
        .subscribe({
          next: (res: any) => this.filterTrainings.set(res.data || res || []),
          error: () => this.filterTrainings.set([])
        });
    } else {
      this.filterTrainings.set([]);
    }
  }

  // --- Modal Form Actions ---
  public openCreateModal(): void {
    this.editingSubject.set(null);
    this.subjectForm.reset({
      name: '',
      code: '',
      levelId: 0,
      trainingId: 0,
      defaultCoefficient: 1,
      theoryHours: 0,
      practicalHours: 0
    });

    this.subjectForm.controls.code.enable();
    this.subjectForm.controls.levelId.enable();
    this.subjectForm.controls.trainingId.disable();

    this.availableTrainings.set([]);
    this.modalErrorMessage.set(null);
    this.isModalOpen.set(true);
  }

  public openEditModal(subject: SubjectResponse): void {
    this.editingSubject.set(subject);

    this.subjectForm.patchValue({
      name: subject.name,
      code: subject.code,
      trainingId: subject.trainingId,
      defaultCoefficient: subject.defaultCoefficient,
      theoryHours: subject.theoryHours,
      practicalHours: subject.practicalHours
    });

    // In edit mode, levelId is not necessarily tracked directly if the API doesn't return it,
    // but we disable structural selections according to your original business logic
    this.subjectForm.controls.code.disable();
    this.subjectForm.controls.levelId.disable();
    this.subjectForm.controls.trainingId.disable();

    this.modalErrorMessage.set(null);
    this.isModalOpen.set(true);
  }

  public closeModal(): void {
    this.isModalOpen.set(false);
  }

  public onSubmitModal(): void {
    if (this.subjectForm.invalid) {
      this.subjectForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.modalErrorMessage.set(null);

    const formValue = this.subjectForm.getRawValue();

    const request: SubjectRequest = {
      name: formValue.name,
      code: formValue.code,
      trainingId: formValue.trainingId,
      defaultCoefficient: formValue.defaultCoefficient,
      theoryHours: formValue.theoryHours,
      practicalHours: formValue.practicalHours,
      description: ''
    };

    const apiCall = this.isEditMode()
      ? this.subjectApiService.updateSubject(this.editingSubject()!.id!, request)
      : this.subjectApiService.createSubject(request);

    apiCall.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => {
        this.closeModal();
        this.fetchSubjectsFromApi();
      },
      error: (err: HttpErrorResponse) => {
        if (err.error && err.error.message === 'SUBJECT_CODE_ALREADY_EXISTS') {
          this.modalErrorMessage.set(
            'Le code saisi existe déjà. Veuillez utiliser un code unique.'
          );
          this.subjectForm.controls.code.setErrors({notUnique: true});
        } else {
          this.modalErrorMessage.set('Une erreur inattendue est survenue.');
        }
      }
    });
  }
}
