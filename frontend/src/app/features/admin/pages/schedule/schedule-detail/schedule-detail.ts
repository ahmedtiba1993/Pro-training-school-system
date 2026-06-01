import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { TimetableSlotControllerService } from '../../../../../core/api/api/timetable-slot-controller.service';
import { RoomControllerService } from '../../../../../core/api/api/room-controller.service';
import { SubjectControllerService } from '../../../../../core/api/api/subject-controller.service';
import { RefTeacherSpecialtyControllerService } from '../../../../../core/api/api/ref-teacher-specialty-controller.service';
import { TeacherControllerService } from '../../../../../core/api/api/teacher-controller.service';
import { ScheduleInfoResponse } from '../../../../../core/api/model/schedule-info-response';
import { TimeSlotDefinitionResponse } from '../../../../../core/api/model/time-slot-definition-response';
import { TimetableSlotInfoResponse } from '../../../../../core/api/model/timetable-slot-info-response';
import { SubjectShortResponse } from '../../../../../core/api/model/subject-short-response';
import { RefTeacherSpecialtySimpleResponse } from '../../../../../core/api/model/ref-teacher-specialty-simple-response';
import { TeacherSimpleResponse } from '../../../../../core/api/model/teacher-simple-response';
import { RoomResponse } from '../../../../../core/api/model/room-response';
import { TimetableSlotDetailResponse } from '../../../../../core/api/model/timetable-slot-detail-response';
import { ToastService } from '../../../../../shared/services/toast.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-schedule-detail',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './schedule-detail.html',
  styleUrl: './schedule-detail.css'
})
export class ScheduleDetail implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly timetableApiService = inject(TimetableSlotControllerService);
  private readonly roomApiService = inject(RoomControllerService);
  private readonly subjectApiService = inject(SubjectControllerService);
  private readonly refSpecialtyApiService = inject(RefTeacherSpecialtyControllerService);
  private readonly teacherApiService = inject(TeacherControllerService);
  private readonly toastService = inject(ToastService);

  readonly scheduleId = signal<number | null>(null);
  readonly scheduleInfo = signal<ScheduleInfoResponse | null>(null);
  readonly timeSlotDefinitions = signal<TimeSlotDefinitionResponse[]>([]);
  readonly timetableSlots = signal<any[]>([]);
  readonly isTeachersLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly isExporting = signal<boolean>(false);
  readonly editingSlotId = signal<number | null>(null);
  readonly isEditMode = computed(() => this.editingSlotId() !== null);
  private firstRoomId: number | null = null;

  readonly days = [
    { code: 'MONDAY', label: 'Lundi' },
    { code: 'TUESDAY', label: 'Mardi' },
    { code: 'WEDNESDAY', label: 'Mercredi' },
    { code: 'THURSDAY', label: 'Jeudi' },
    { code: 'FRIDAY', label: 'Vendredi' },
    { code: 'SATURDAY', label: 'Samedi' }
  ];

  readonly specialties = signal<RefTeacherSpecialtySimpleResponse[]>([]);

  readonly teachers = signal<TeacherSimpleResponse[]>([]);

  readonly subjects = signal<SubjectShortResponse[]>([]);

  readonly rooms = signal<RoomResponse[]>([]);

  readonly isAddModalOpen = signal<boolean>(false);
  readonly selectedDay = signal<string>('');
  readonly selectedSlotId = signal<number | null>(null);

  readonly isDetailModalOpen = signal<boolean>(false);
  readonly isDetailLoading = signal<boolean>(false);
  readonly activeSlotDetail = signal<TimetableSlotDetailResponse | null>(null);

  readonly addSlotForm = this.fb.nonNullable.group({
    specialtyId: ['', [Validators.required]],
    subjectId: ['', [Validators.required]],
    teacherId: ['', [Validators.required]],
    roomId: ['', [Validators.required]],
    periodicity: ['NORMAL', [Validators.required]]
  });

  formatTime(timeStr: string | undefined): string {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
      return `${parts[0]}h${parts[1]}`;
    }
    return timeStr;
  }

  getSlotItem(dayOfWeek: string, timeSlotDefId: number | undefined): any {
    if (timeSlotDefId === undefined || timeSlotDefId === null) return undefined;
    return this.timetableSlots().find(
      slot => slot.dayOfWeek === dayOfWeek && slot.timeSlotDefinition?.id === timeSlotDefId
    );
  }

  getSlotItems(dayOfWeek: string, timeSlotDefId: number | undefined): any[] {
    if (timeSlotDefId === undefined || timeSlotDefId === null) return [];
    return this.timetableSlots().filter(
      slot => slot.dayOfWeek === dayOfWeek && slot.timeSlotDefinition?.id === timeSlotDefId
    );
  }

  getCardStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'bg-slate-50 border-slate-200';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'bg-indigo-50 border-indigo-100 hover:border-indigo-300';
    if (sub.includes('archi')) return 'bg-emerald-50 border-emerald-100 hover:border-emerald-300';
    if (sub.includes('syst')) return 'bg-blue-50 border-blue-100 hover:border-blue-300';
    if (sub.includes('français') || sub.includes('francais'))
      return 'bg-purple-50 border-purple-100 hover:border-purple-300';
    if (sub.includes('base') || sub.includes('donn'))
      return 'bg-rose-50 border-rose-100 hover:border-rose-300';
    if (sub.includes('anglais')) return 'bg-teal-50 border-teal-100 hover:border-teal-300';
    return 'bg-slate-50 border-slate-200';
  }

  getTextStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'text-slate-900';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'text-indigo-900';
    if (sub.includes('archi')) return 'text-emerald-950';
    if (sub.includes('syst')) return 'text-blue-900';
    if (sub.includes('français') || sub.includes('francais')) return 'text-purple-900';
    if (sub.includes('base') || sub.includes('donn')) return 'text-rose-950';
    if (sub.includes('anglais')) return 'text-teal-950';
    return 'text-slate-900';
  }

  getBadgeStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'text-slate-700';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'text-indigo-700';
    if (sub.includes('archi')) return 'text-emerald-700';
    if (sub.includes('syst')) return 'text-blue-700';
    if (sub.includes('français') || sub.includes('francais')) return 'text-purple-700';
    if (sub.includes('base') || sub.includes('donn')) return 'text-rose-700';
    if (sub.includes('anglais')) return 'text-teal-700';
    return 'text-slate-700';
  }

  getBadgeBgStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'bg-slate-100';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'bg-indigo-100';
    if (sub.includes('archi')) return 'bg-emerald-100';
    if (sub.includes('syst')) return 'bg-blue-100';
    if (sub.includes('français') || sub.includes('francais')) return 'bg-purple-100';
    if (sub.includes('base') || sub.includes('donn')) return 'bg-rose-100';
    if (sub.includes('anglais')) return 'bg-teal-100';
    return 'bg-slate-100';
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idStr = params.get('id');
      if (idStr) {
        const id = Number(idStr);
        this.scheduleId.set(id);
        this.loadTimetableView(id);
      }
    });

    this.roomApiService.getActiveRooms().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.rooms.set(res.data);
          if (res.data.length > 0) {
            this.firstRoomId = res.data[0].id || null;
          }
        }
      },
      error: err => console.error('Erreur lors du chargement des salles actives:', err)
    });

    // Charger la liste des spécialités
    this.refSpecialtyApiService.getAllTeacherSpecialtiesSimple().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.specialties.set(res.data);
        }
      },
      error: err => console.error('Erreur lors du chargement des spécialités:', err)
    });

    // Charger les profs dynamiquement en fonction de la spécialité choisie
    this.addSlotForm.get('specialtyId')?.valueChanges.subscribe(val => {
      if (val) {
        this.loadTeachersBySpecialty(Number(val));
      } else {
        this.teachers.set([]);
      }
      this.addSlotForm.get('teacherId')?.setValue('');
    });
  }

  loadTeachersBySpecialty(specialtyId: number, callback?: () => void): void {
    this.isTeachersLoading.set(true);
    this.refSpecialtyApiService.getActiveTeachersBySpecialty(specialtyId).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.teachers.set(res.data);
          if (callback) {
            callback();
          }
        }
        this.isTeachersLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement des profs par spécialité:', err);
        this.teachers.set([]);
        this.isTeachersLoading.set(false);
      }
    });
  }

  loadTimetableView(scheduleId: number): void {
    this.timetableApiService.getTimetableView(scheduleId).subscribe({
      next: response => {
        if (response.success && response.data) {
          const info = response.data.scheduleInfo || null;
          this.scheduleInfo.set(info);
          this.timeSlotDefinitions.set(response.data.timeSlotDefinitions || []);
          this.timetableSlots.set(response.data.timetableSlots || []);

          if (info && info.promotionId) {
            this.loadSubjectsByPromotion(info.promotionId);
          }
        }
      },
      error: err => {
        console.error("Erreur lors du chargement de l'emploi du temps:", err);
        this.toastService.error('Impossible de charger le planning.');
      }
    });
  }

  loadSubjectsByPromotion(promotionId: number): void {
    this.subjectApiService.getCatalogSubjectsByPromotionId(promotionId).subscribe({
      next: response => {
        if (response.success && response.data) {
          this.subjects.set(response.data);
        }
      },
      error: err => {
        console.error('Erreur lors du chargement des matières de la promotion:', err);
      }
    });
  }

  openAddModal(
    dayOfWeek: string,
    timeSlotDefId: number | undefined,
    defaultPeriodicity: 'NORMAL' | 'WEEK_A' | 'WEEK_B' = 'NORMAL'
  ): void {
    if (timeSlotDefId === undefined || timeSlotDefId === null) return;
    this.selectedDay.set(dayOfWeek);
    this.selectedSlotId.set(timeSlotDefId);
    this.addSlotForm.reset({
      specialtyId: '',
      teacherId: '',
      subjectId: '',
      roomId: '',
      periodicity: defaultPeriodicity
    });
    this.isAddModalOpen.set(true);
  }

  closeAddModal(): void {
    this.isAddModalOpen.set(false);
    this.editingSlotId.set(null);
  }

  openDetailModal(slotId: number | undefined): void {
    if (slotId === undefined || slotId === null) return;
    this.isDetailLoading.set(true);
    this.isDetailModalOpen.set(true);

    this.timetableApiService.getSlotById(slotId).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.activeSlotDetail.set(res.data);
        } else {
          this.toastService.error('Erreur lors du chargement des détails.');
          this.closeDetailModal();
        }
        this.isDetailLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement du cours:', err);
        this.toastService.error('Impossible de récupérer les détails du cours.');
        this.closeDetailModal();
        this.isDetailLoading.set(false);
      }
    });
  }

  closeDetailModal(): void {
    this.isDetailModalOpen.set(false);
    this.activeSlotDetail.set(null);
  }

  getDayLabel(dayCode: string | undefined): string {
    if (!dayCode) return '';
    return this.days.find(d => d.code === dayCode)?.label || dayCode;
  }

  onEditClick(): void {
    const detail = this.activeSlotDetail();
    if (!detail || !detail.id) return;

    const slotInfo = this.timetableSlots().find(s => s.id === detail.id);
    if (!slotInfo) {
      this.toastService.error('Impossible de trouver les informations du cours.');
      return;
    }

    const currentTeacherId = slotInfo.teacher?.id;
    const currentSubjectId = slotInfo.subject?.id;
    const currentRoomId = slotInfo.room?.id;
    const currentPeriodicity = slotInfo.periodicity || 'NORMAL';
    const currentDay = slotInfo.dayOfWeek || '';
    const currentSlotId = slotInfo.timeSlotDefinition?.id || null;

    this.closeDetailModal();

    this.editingSlotId.set(detail.id);
    this.selectedDay.set(currentDay);
    this.selectedSlotId.set(currentSlotId);

    this.addSlotForm.patchValue({
      subjectId: currentSubjectId ? String(currentSubjectId) : '',
      roomId: currentRoomId ? String(currentRoomId) : '',
      periodicity: currentPeriodicity,
      specialtyId: '',
      teacherId: ''
    }, { emitEvent: false });

    this.isAddModalOpen.set(true);

    if (currentTeacherId) {
      this.teacherApiService.getTeacherById(currentTeacherId).subscribe({
        next: res => {
          if (res.success && res.data) {
            const specs = Array.from(res.data.specialties || []);
            const specialtyId = specs.length > 0 ? specs[0].id : null;
            if (specialtyId) {
              this.loadTeachersBySpecialty(specialtyId, () => {
                this.addSlotForm.patchValue({
                  specialtyId: String(specialtyId),
                  teacherId: String(currentTeacherId)
                }, { emitEvent: false });
              });
            }
          }
        },
        error: err => {
          console.error('Erreur lors du chargement des détails de l\'enseignant:', err);
        }
      });
    }
  }

  handleAddSlotSubmit(): void {
    if (this.addSlotForm.invalid || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    const val = this.addSlotForm.getRawValue();
    const currentScheduleId = this.scheduleId();
    if (!currentScheduleId) {
      this.isSubmitting.set(false);
      return;
    }

    const request = {
      dayOfWeek: this.selectedDay() as any,
      periodicity: val.periodicity as any,
      scheduleId: currentScheduleId,
      subjectId: Number(val.subjectId),
      teacherId: Number(val.teacherId),
      roomId: Number(val.roomId),
      timeSlotDefinitionId: this.selectedSlotId()!
    };

    const editId = this.editingSlotId();
    if (editId !== null) {
      this.timetableApiService.updateSlot(editId, request).subscribe({
        next: () => {
          this.toastService.success('Le cours a été mis à jour avec succès !');
          this.loadTimetableView(currentScheduleId);
          this.closeAddModal();
          this.isSubmitting.set(false);
        },
        error: err => {
          console.error('Erreur lors de la mise à jour:', err);
          const body = err?.error;
          if (body?.errorCode === 'ERR_VALIDATION') {
            const msg = this.mapValidationMessage(body.message);
            this.toastService.error(msg);
          } else {
            this.toastService.error('Erreur lors de la mise à jour du cours.');
          }
          this.isSubmitting.set(false);
        }
      });
    } else {
      this.timetableApiService.createSlot(request).subscribe({
        next: () => {
          this.toastService.success('Le cours a été planifié avec succès !');
          this.loadTimetableView(currentScheduleId);
          this.closeAddModal();
          this.isSubmitting.set(false);
        },
        error: err => {
          console.error('Erreur lors de la planification:', err);
          const body = err?.error;
          if (body?.errorCode === 'ERR_VALIDATION') {
            const msg = this.mapValidationMessage(body.message);
            this.toastService.error(msg);
          } else {
            this.toastService.error('Erreur lors de la planification du cours.');
          }
          this.isSubmitting.set(false);
        }
      });
    }
  }

  private mapValidationMessage(code: string): string {
    const messages: Record<string, string> = {
      TEACHER_SLOT_CONFLICT:
        'Conflit : cet enseignant est déjà affecté à un autre cours sur ce créneau.',
      ROOM_SLOT_CONFLICT: 'Conflit : cette salle est déjà occupée sur ce créneau.'
    };
    return messages[code] ?? `Erreur de validation : ${code}`;
  }

  exportToPdf(scheduleId: number): void {
    if (this.isExporting()) return;
    this.isExporting.set(true);

    this.timetableApiService
      .exportTimetablePdfBySchedule(scheduleId, 'body', false, {
        httpHeaderAccept: 'application/pdf' as any
      })
      .pipe(finalize(() => this.isExporting.set(false)))
      .subscribe({
        next: (response: any) => {
          try {
            const blob =
              response instanceof Blob
                ? response
                : new Blob([response], { type: 'application/pdf' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `emploi_du_temps_schedule_${scheduleId}.pdf`;
            link.click();
            window.URL.revokeObjectURL(url);
            this.toastService.success('Export PDF réussi !');
          } catch (e) {
            console.error('Error processing PDF blob:', e);
            this.toastService.error('Erreur lors du traitement du fichier PDF.');
          }
        },
        error: err => {
          console.error("Erreur lors de l'export PDF:", err);
        }
      });
  }
}
