import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { 
  ExamTimetableControllerService, 
  ExamScheduleControllerService,
  AssessmentControllerService,
  RoomControllerService,
  ExamTimeSlotControllerService,
  ExamTimetableResponse,
  ExamScheduleResponse,
  ExamScheduleRequest,
  AssessmentLookupResponse,
  RoomResponse,
  ExamTimeSlotResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-exam-timetable-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, ReactiveFormsModule],
  templateUrl: './exam-timetable-detail.html',
  styleUrl: './exam-timetable-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamTimetableDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly examTimetableApiService = inject(ExamTimetableControllerService);
  private readonly examScheduleApiService = inject(ExamScheduleControllerService);
  private readonly assessmentApiService = inject(AssessmentControllerService);
  private readonly roomApiService = inject(RoomControllerService);
  private readonly examTimeSlotApiService = inject(ExamTimeSlotControllerService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly timetableId = signal<number | null>(null);
  readonly timetable = signal<ExamTimetableResponse | null>(null);
  readonly schedules = signal<ExamScheduleResponse[]>([]);
  readonly unscheduledAssessments = signal<AssessmentLookupResponse[]>([]);
  readonly rooms = signal<RoomResponse[]>([]);
  readonly timeSlots = signal<ExamTimeSlotResponse[]>([]);
  readonly showPublishModal = signal<boolean>(false);
  readonly isPublishing = signal<boolean>(false);
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly showModal = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);

  readonly scheduleForm = this.fb.nonNullable.group({
    assessmentId: [null as number | null, [Validators.required, Validators.min(1)]],
    examDate: ['', [Validators.required]],
    examTimeSlotId: [null as number | null, [Validators.required, Validators.min(1)]],
    roomId: [null as number | null],
    capacityUsed: [null as number | null, [Validators.min(0)]]
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.timetableId.set(Number(idParam));
      this.loadDetails();
    } else {
      this.errorMsg.set('ID du calendrier d\'examens manquant.');
    }
  }

  loadDetails(): void {
    const id = this.timetableId();
    if (!id) return;

    this.isLoading.set(true);
    this.errorMsg.set(null);

    // Fetch Timetable Metadata
    this.examTimetableApiService.getTimetableById(id).subscribe({
      next: resTimetable => {
        this.timetable.set(resTimetable.data || null);
        
        // Fetch Schedules list
        this.examScheduleApiService.getSchedulesByTimetableId(id).subscribe({
          next: resSchedules => {
            this.schedules.set(resSchedules.data || []);
            this.isLoading.set(false);
          },
          error: err => {
            console.error('Erreur chargement épreuves:', err);
            this.toastService.error('Impossible de charger les épreuves du calendrier.');
            this.isLoading.set(false);
          }
        });
      },
      error: err => {
        console.error('Erreur chargement calendrier:', err);
        this.errorMsg.set('Impossible de charger le calendrier d\'examens.');
        this.toastService.error('Erreur lors du chargement des détails.');
        this.isLoading.set(false);
      }
    });
  }

  openPublishConfirmModal(): void {
    this.showPublishModal.set(true);
  }

  closePublishConfirmModal(): void {
    this.showPublishModal.set(false);
    this.isPublishing.set(false);
  }

  confirmPublish(): void {
    const id = this.timetableId();
    if (!id || this.isPublishing()) return;

    this.isPublishing.set(true);
    this.examTimetableApiService.publishTimetable(id).subscribe({
      next: () => {
        this.isPublishing.set(false);
        this.closePublishConfirmModal();
        this.toastService.success('Calendrier d\'examen publié avec succès !');
        this.loadDetails();
      },
      error: err => {
        this.isPublishing.set(false);
        console.error('Erreur de publication:', err);
        this.toastService.error('Impossible de publier le calendrier d\'examen.');
      }
    });
  }

  openAddScheduleModal(): void {
    const timetableId = this.timetableId();
    if (!timetableId) {
      this.toastService.error('ID du calendrier manquant.');
      return;
    }

    this.scheduleForm.reset({
      assessmentId: null,
      examDate: '',
      examTimeSlotId: null,
      roomId: null,
      capacityUsed: null
    });

    forkJoin({
      assessments: this.assessmentApiService.getUnscheduledAssessmentsForTimetable(timetableId),
      rooms: this.roomApiService.getActiveRooms(),
      slots: this.examTimeSlotApiService.getActiveExamTimeSlots()
    }).subscribe({
      next: ({ assessments, rooms, slots }) => {
        this.unscheduledAssessments.set(assessments.data || []);
        this.rooms.set(rooms.data || []);
        this.timeSlots.set(slots.data || []);
        this.showModal.set(true);
      },
      error: err => {
        console.error('Erreur lors du chargement des données (évaluations/salles/créneaux) :', err);
        this.toastService.error('Impossible de charger les listes d\'évaluations, de salles et de créneaux.');
      }
    });
  }

  closeAddScheduleModal(): void {
    this.showModal.set(false);
  }

  onSubmitSchedule(): void {
    if (this.scheduleForm.invalid || this.isSubmitting()) return;

    const timetableId = this.timetableId();
    if (!timetableId) {
      this.toastService.error('ID du calendrier manquant.');
      return;
    }

    const val = this.scheduleForm.getRawValue();
    if (!val.assessmentId || !val.examDate || !val.examTimeSlotId) {
      this.toastService.error('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    this.isSubmitting.set(true);

    const payload: ExamScheduleRequest = {
      assessmentId: val.assessmentId,
      examDate: val.examDate,
      examTimeSlotId: val.examTimeSlotId,
      roomId: val.roomId ?? undefined,
      capacityUsed: val.capacityUsed ?? undefined
    };

    this.examScheduleApiService.createSchedule1(timetableId, payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeAddScheduleModal();
        this.toastService.success('Épreuve ajoutée avec succès !');
        this.loadDetails();
      },
      error: err => {
        this.isSubmitting.set(false);
        console.error('Erreur lors de la création de l\'épreuve:', err);
        const msg = err.error?.message || err.error?.errorCode;
        let frenchMsg = 'Une erreur est survenue lors de la planification.';
        if (msg === 'ASSESSMENT_ID_REQUIRED' || msg === 'ASSESSMENT_NOT_FOUND') {
          frenchMsg = 'L\'évaluation sélectionnée est invalide ou obligatoire.';
        } else if (msg === 'EXAM_DATE_REQUIRED') {
          frenchMsg = 'La date de l\'examen est obligatoire.';
        } else if (msg === 'EXAM_TIME_SLOT_ID_REQUIRED' || msg === 'EXAM_TIME_SLOT_NOT_FOUND') {
          frenchMsg = 'Le créneau horaire est invalide ou obligatoire.';
        } else if (msg === 'CLASS_ALREADY_SCHEDULED_AT_THIS_TIME') {
          frenchMsg = 'Cette classe a déjà un examen planifié à cette date et ce créneau.';
        } else if (msg === 'ASSESSMENT_ALREADY_SCHEDULED_IN_THIS_TIMETABLE') {
          frenchMsg = 'Cette épreuve est déjà planifiée dans ce calendrier.';
        } else if (msg === 'NOT_AN_OFFICIAL_EXAM') {
          frenchMsg = 'L\'évaluation doit être un examen officiel (Examen Principal ou Rattrapage).';
        } else if (msg === 'TIMETABLE_ALREADY_PUBLISHED') {
          frenchMsg = 'Le calendrier est déjà publié et ne peut plus être modifié.';
        }
        this.toastService.error(frenchMsg);
      }
    });
  }
}
