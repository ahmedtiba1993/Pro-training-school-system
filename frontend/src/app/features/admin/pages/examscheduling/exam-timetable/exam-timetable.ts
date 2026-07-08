import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { 
  ExamTimetableControllerService,
  ClassGroupControllerService,
  PeriodControllerService,
  ExamSessionControllerService,
  ExamTimetableResponse,
  ClassGroupResponse,
  DefaultPeriodResponse,
  ExamSessionResponse,
  ExamTimetableRequest
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-exam-timetable',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './exam-timetable.html',
  styleUrl: './exam-timetable.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamTimetable implements OnInit {
  private readonly examTimetableApiService = inject(ExamTimetableControllerService);
  private readonly classGroupApiService = inject(ClassGroupControllerService);
  private readonly periodApiService = inject(PeriodControllerService);
  private readonly examSessionApiService = inject(ExamSessionControllerService);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly timetables = signal<ExamTimetableResponse[]>([]);
  readonly classGroups = signal<ClassGroupResponse[]>([]);
  readonly periods = signal<DefaultPeriodResponse[]>([]);
  readonly examSessions = signal<ExamSessionResponse[]>([]);

  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);

  readonly showModal = signal<boolean>(false);
  readonly showPublishModal = signal<boolean>(false);
  readonly timetableIdToPublish = signal<number | null>(null);
  readonly isPublishing = signal<boolean>(false);
  readonly searchQuery = signal<string>('');
  readonly selectedStatus = signal<string>('');

  readonly timetableForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    classGroupId: [null as number | null, [Validators.required]],
    periodId: [null as number | null, [Validators.required]],
    examSessionId: [null as number | null, [Validators.required]]
  });
  readonly filteredTimetables = computed(() => {
    let list = this.timetables();
    const query = this.searchQuery().toLowerCase().trim();
    const status = this.selectedStatus();

    if (query) {
      list = list.filter(
        t =>
          t.name?.toLowerCase().includes(query) ||
          t.classGroupName?.toLowerCase().includes(query) ||
          t.periodLabel?.toLowerCase().includes(query) ||
          t.examSessionLabel?.toLowerCase().includes(query)
      );
    }

    if (status) {
      list = list.filter(t => t.status === status);
    }

    return list;
  });

  ngOnInit(): void {
    this.loadTimetables();
    this.loadSelectData();
    this.setupFormValueChanges();
  }

  loadTimetables(): void {
    this.isLoading.set(true);
    this.errorMsg.set(null);

    this.examTimetableApiService.getAllTimetables().subscribe({
      next: response => {
        this.timetables.set(response.data || []);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement des calendriers d\'examens:', err);
        this.errorMsg.set('Impossible de charger les calendriers d\'examens pour le moment.');
        this.toastService.error('Impossible de charger les calendriers d\'examens.');
        this.isLoading.set(false);
      }
    });
  }

  loadSelectData(): void {
    // Load active class groups
    this.classGroupApiService.getAllClassGroups(undefined, undefined, 'ACTIVE').subscribe({
      next: response => {
        this.classGroups.set(response.data || []);
      },
      error: err => console.error('Erreur lors du chargement des classes:', err)
    });

    // Load periods of default academic year
    this.periodApiService.getPeriodsOfDefaultAcademicYear().subscribe({
      next: response => {
        this.periods.set(response.data || []);
      },
      error: err => console.error('Erreur lors du chargement des périodes:', err)
    });
  }

  setupFormValueChanges(): void {
    // Watch for period changes to reload exam sessions dynamically
    this.timetableForm.get('periodId')?.valueChanges.subscribe(periodId => {
      this.examSessions.set([]);
      this.timetableForm.patchValue({ examSessionId: null });

      if (periodId) {
        this.examSessionApiService.getExamSessionsByPeriodId(periodId).subscribe({
          next: response => {
            this.examSessions.set(response.data || []);
          },
          error: err => {
            console.error('Erreur lors du chargement des sessions d\'examens:', err);
            this.toastService.error('Impossible de charger les sessions d\'examens pour cette période.');
          }
        });
      }
    });
  }

  openPublishConfirmModal(id: number | undefined): void {
    if (id === undefined) return;
    this.timetableIdToPublish.set(id);
    this.showPublishModal.set(true);
  }

  closePublishConfirmModal(): void {
    this.showPublishModal.set(false);
    this.timetableIdToPublish.set(null);
    this.isPublishing.set(false);
  }

  confirmPublish(): void {
    const id = this.timetableIdToPublish();
    if (!id || this.isPublishing()) return;

    this.isPublishing.set(true);
    this.examTimetableApiService.publishTimetable(id).subscribe({
      next: () => {
        this.isPublishing.set(false);
        this.closePublishConfirmModal();
        this.toastService.success('Calendrier d\'examen publié avec succès !');
        this.loadTimetables();
      },
      error: err => {
        this.isPublishing.set(false);
        console.error('Erreur lors de la publication du calendrier:', err);
        this.toastService.error('Impossible de publier le calendrier d\'examen.');
      }
    });
  }

  openCreateModal(): void {
    this.timetableForm.reset({
      name: '',
      classGroupId: null,
      periodId: null,
      examSessionId: null
    });
    this.examSessions.set([]);
    this.showModal.set(true);
  }

  closeCreateModal(): void {
    this.showModal.set(false);
  }

  onSubmit(): void {
    if (this.timetableForm.invalid || this.isSubmitting()) return;

    const val = this.timetableForm.getRawValue();
    if (!val.classGroupId || !val.periodId || !val.examSessionId) {
      this.toastService.error('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    this.isSubmitting.set(true);

    const payload: ExamTimetableRequest = {
      name: val.name.trim(),
      classGroupId: val.classGroupId,
      periodId: val.periodId,
      examSessionId: val.examSessionId
    };

    this.examTimetableApiService.createTimetable(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeCreateModal();
        this.toastService.success('Calendrier d\'examen créé avec succès !');
        this.loadTimetables();
      },
      error: err => {
        this.isSubmitting.set(false);
        console.error('Erreur lors de la création du calendrier:', err);
        const msg = err.error?.message || err.error?.errorCode;
        let frenchMsg = 'Une erreur est survenue lors de la création.';
        if (msg === 'NAME_REQUIRED') {
          frenchMsg = 'Le nom du calendrier est obligatoire.';
        } else if (msg === 'CLASS_GROUP_ID_REQUIRED') {
          frenchMsg = 'La classe est obligatoire.';
        } else if (msg === 'PERIOD_ID_REQUIRED') {
          frenchMsg = 'La période est obligatoire.';
        } else if (msg === 'EXAM_SESSION_ID_REQUIRED') {
          frenchMsg = 'La session d\'examen est obligatoire.';
        }
        this.toastService.error(frenchMsg);
      }
    });
  }
}
