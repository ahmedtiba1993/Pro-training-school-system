import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router'; // 🚀 NEW IMPORT for navigation

import { ScheduleControllerService } from '../../../../../core/api/api/schedule-controller.service';
import { TimetableSlotControllerService } from '../../../../../core/api/api/timetable-slot-controller.service';
import { ScheduleResponse } from '../../../../../core/api/model/schedule-response';
import { ScheduleStatusRequest } from '../../../../../core/api/model/schedule-status-request';
import { ClassGroupControllerService } from '../../../../../core/api/api/class-group-controller.service';
import { ActiveClassGroupResponse } from '../../../../../core/api/model/active-class-group-response';
import { PeriodControllerService } from '../../../../../core/api/api/period-controller.service';
import { DefaultPeriodResponse } from '../../../../../core/api/model/default-period-response';
import { ToastService } from '../../../../../shared/services/toast.service';
import { finalize } from 'rxjs';

export type ScheduleStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export type TrainingType = 'CONTINUOUS' | 'ACCELERATED' | 'ACCREDITED';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], // 🚀 ADDED HERE
  templateUrl: './schedule-list.html',
  styleUrls: ['./schedule-list.css']
})
export class ScheduleList implements OnInit {
  private readonly scheduleApiService = inject(ScheduleControllerService);
  private readonly timetableApiService = inject(TimetableSlotControllerService);
  private readonly classGroupApiService = inject(ClassGroupControllerService);
  private readonly periodApiService = inject(PeriodControllerService);
  private readonly toastService = inject(ToastService);

  // --- MAIN STATE SIGNALS ---
  readonly schedules = signal<ScheduleResponse[]>([]);
  readonly selectedStatus = signal<ScheduleStatus>('ACTIVE');
  readonly searchQuery = signal<string>('');
  readonly isLoading = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);
  readonly isExporting = signal<boolean>(false);
  readonly exportingScheduleId = signal<number | null>(null);

  // --- POPUP MODAL SIGNALS ---
  readonly isModalOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly isClassesLoading = signal<boolean>(false);
  readonly isPeriodsLoading = signal<boolean>(false);

  readonly activeClasses = signal<ActiveClassGroupResponse[]>([]);
  readonly defaultPeriods = signal<DefaultPeriodResponse[]>([]);

  readonly scheduleLabel = signal<string>('');
  readonly selectedClassId = signal<string>('');
  readonly selectedPeriodId = signal<string>('');

  readonly editingScheduleId = signal<number | null>(null);
  readonly isEditMode = computed(() => this.editingScheduleId() !== null);

  // --- STATUS CHANGE SIGNALS ---
  readonly isStatusModalOpen = signal<boolean>(false);
  readonly statusModalSchedule = signal<ScheduleResponse | null>(null);
  readonly statusModalNewStatus = signal<ScheduleStatus | null>(null);
  readonly isStatusSubmitting = signal<boolean>(false);

  readonly showPeriodField = computed(() => {
    if (this.isEditMode()) return false;
    const classId = Number(this.selectedClassId());
    if (!classId) return false;
    const matchedClass = this.activeClasses().find(cls => cls.id === classId);
    return matchedClass?.trainingType === 'ACCREDITED';
  });

  readonly filteredSchedules = computed(() => {
    const query = this.searchQuery()
      .toLowerCase()
      .trim();
    const currentSchedules = this.schedules();
    if (!query) return currentSchedules;

    return currentSchedules.filter(
      item =>
        item.classGroupName?.toLowerCase().includes(query) ||
        item.trainingName?.toLowerCase().includes(query) ||
        item.label?.toLowerCase().includes(query)
    );
  });

  ngOnInit(): void {
    this.loadSchedules(this.selectedStatus());
  }

  loadSchedules(status: ScheduleStatus): void {
    this.isLoading.set(true);
    this.errorMsg.set(null);

    this.scheduleApiService.findAllByStatus(status).subscribe({
      next: response => {
        this.schedules.set(response.data || []);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur API Emplois du temps:', err);
        this.errorMsg.set('Impossible de charger les plannings pour le moment.');
        this.schedules.set([]);
        this.isLoading.set(false);
      }
    });
  }

  loadActiveClasses(): void {
    this.isClassesLoading.set(true);
    this.classGroupApiService.getAllActiveClasses().subscribe({
      next: response => {
        this.activeClasses.set(response.data || []);
        this.isClassesLoading.set(false);
      },
      error: () => this.isClassesLoading.set(false)
    });
  }

  loadDefaultPeriods(): void {
    this.isPeriodsLoading.set(true);
    this.periodApiService.getPeriodsOfDefaultAcademicYear().subscribe({
      next: response => {
        this.defaultPeriods.set(response.data || []);
        this.isPeriodsLoading.set(false);
      },
      error: () => this.isPeriodsLoading.set(false)
    });
  }

  changeStatusTab(status: ScheduleStatus): void {
    this.selectedStatus.set(status);
    this.loadSchedules(status);
  }

  handleFormSubmit(): void {
    if (this.isEditMode()) {
      this.onUpdateSubmit();
    } else {
      this.onCreateSubmit();
    }
  }

  onCreateSubmit(): void {
    if (!this.scheduleLabel().trim() || !this.selectedClassId()) return;
    this.isSubmitting.set(true);

    const payload = {
      label: this.scheduleLabel().trim(),
      classGroupId: Number(this.selectedClassId()),
      periodId:
        this.showPeriodField() && this.selectedPeriodId()
          ? Number(this.selectedPeriodId())
          : undefined
    };

    this.scheduleApiService.createSchedule(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeModal();
        this.toastService.success("L'emploi du temps a été créé avec succès !");
        this.loadSchedules(this.selectedStatus());
      },
      error: err => this.handleApiError(err)
    });
  }

  onUpdateSubmit(): void {
    const id = this.editingScheduleId();
    if (!id || !this.scheduleLabel().trim()) return;

    this.isSubmitting.set(true);
    const request = { label: this.scheduleLabel().trim() };

    this.scheduleApiService.updateSchedule(id, request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeModal();
        this.toastService.success('Libellé mis à jour avec succès !');
        this.loadSchedules(this.selectedStatus());
      },
      error: err => this.handleApiError(err)
    });
  }

  private handleApiError(err: any): void {
    this.isSubmitting.set(false);
    const msg = err.error?.message;
    let frenchMsg = 'Une erreur est survenue.';

    if (msg === 'SCHEDULE_ALREADY_EXISTS_FOR_THIS_CONTINUOUS_CLASS') {
      frenchMsg = 'Un emploi du temps existe déjà pour cette classe continue.';
    } else if (msg === 'SCHEDULE_ALREADY_EXISTS_FOR_THIS_ACCELERATED_CLASS') {
      frenchMsg = 'Un emploi du temps existe déjà pour cette classe accélérée.';
    } else if (msg === 'SCHEDULE_ALREADY_EXISTS_FOR_THIS_ACCREDITED_CLASS') {
      frenchMsg = 'Un emploi du temps existe déjà pour cette classe homologuée.';
    } else if (msg === 'PERIOD_IS_LOCKED') {
      frenchMsg = 'La période académique sélectionnée est verrouillée.';
    } else if (err.error?.errorCode === 'ENTITY_ALREADY_EXISTS') {
      frenchMsg = 'Cet enregistrement existe déjà dans la base de données.';
    }

    this.toastService.error(frenchMsg);
  }

  openCreateModal(): void {
    this.editingScheduleId.set(null);
    this.scheduleLabel.set('');
    this.selectedClassId.set('');
    this.selectedPeriodId.set('');
    this.isModalOpen.set(true);

    this.loadActiveClasses();
    this.loadDefaultPeriods();
  }

  openEditModal(item: ScheduleResponse): void {
    this.editingScheduleId.set(item.id!);
    this.scheduleLabel.set(item.label || '');
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  openStatusModal(schedule: ScheduleResponse, newStatus: ScheduleStatus): void {
    const currentStatus = schedule.status || 'ACTIVE';

    // Safety check on forbidden transitions in frontend
    if (currentStatus === 'ACTIVE' && newStatus === 'DRAFT') {
      this.toastService.error(
        'Transition interdite : impossible de repasser un emploi actif en brouillon.'
      );
      return;
    }
    if (currentStatus === 'ARCHIVED') {
      this.toastService.error(
        'Transition interdite : un emploi archivé ne peut plus être modifié.'
      );
      return;
    }

    this.statusModalSchedule.set(schedule);
    this.statusModalNewStatus.set(newStatus);
    this.isStatusModalOpen.set(true);
  }

  closeStatusModal(): void {
    this.isStatusModalOpen.set(false);
    this.statusModalSchedule.set(null);
    this.statusModalNewStatus.set(null);
  }

  handleStatusChangeSubmit(): void {
    const schedule = this.statusModalSchedule();
    const newStatus = this.statusModalNewStatus();
    if (!schedule || !schedule.id || !newStatus) return;

    this.isStatusSubmitting.set(true);
    const request = { newStatus: newStatus as any };

    this.scheduleApiService.changeScheduleStatus(schedule.id, request).subscribe({
      next: () => {
        this.isStatusSubmitting.set(false);
        this.closeStatusModal();
        this.toastService.success(`Le statut a été mis à jour avec succès (${newStatus}) !`);
        this.loadSchedules(this.selectedStatus());
      },
      error: err => {
        console.error('Erreur lors du changement de statut:', err);
        this.isStatusSubmitting.set(false);
        const errorMsg = err.error?.message || 'Erreur lors du changement de statut.';
        this.toastService.error(errorMsg);
      }
    });
  }

  onSearchChange(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  getShortCode(name: string | undefined): string {
    if (!name) return 'EDT';
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .substring(0, 3)
      .toUpperCase();
  }

  getTrainingTypeLabel(type: TrainingType | undefined): string {
    const labels: Record<TrainingType, string> = {
      CONTINUOUS: 'Continue',
      ACCELERATED: 'Accélérée',
      ACCREDITED: 'Homologuée'
    };
    return type ? labels[type] : '';
  }

  exportToPdf(scheduleId: number): void {
    if (this.isExporting()) return;
    this.isExporting.set(true);
    this.exportingScheduleId.set(scheduleId);

    this.timetableApiService
      .exportTimetablePdfBySchedule(scheduleId, 'body', false, {
        httpHeaderAccept: 'application/pdf' as any
      })
      .pipe(
        finalize(() => {
          this.isExporting.set(false);
          this.exportingScheduleId.set(null);
        })
      )
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
