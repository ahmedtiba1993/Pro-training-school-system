import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { ExamTimeSlotControllerService } from '../../../../../core/api';
import { ExamTimeSlotResponse } from '../../../../../core/api';
import { ExamTimeSlotRequest } from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-exam-time-slot',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './exam-time-slot.html',
  styleUrl: './exam-time-slot.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamTimeSlot implements OnInit {
  private readonly examTimeSlotApiService = inject(ExamTimeSlotControllerService);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly timeSlots = signal<ExamTimeSlotResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);

  readonly slotForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9_-]+$/)]],
    label: ['', [Validators.required, Validators.minLength(2)]],
    startTime: ['09:00', [Validators.required]],
    endTime: ['12:00', [Validators.required]]
  });

  readonly totalDurationMinutes = computed(() => {
    let total = 0;
    for (const slot of this.timeSlots()) {
      if (slot.startTime && slot.endTime) {
        const startParts = slot.startTime.split(':').map(Number);
        const endParts = slot.endTime.split(':').map(Number);
        if (startParts.length >= 2 && endParts.length >= 2) {
          const startMins = startParts[0] * 60 + startParts[1];
          const endMins = endParts[0] * 60 + endParts[1];
          if (endMins > startMins) {
            total += (endMins - startMins);
          }
        }
      }
    }
    return total;
  });

  readonly formattedTotalDuration = computed(() => {
    const mins = this.totalDurationMinutes();
    const hours = Math.floor(mins / 60);
    const remainingMins = mins % 60;
    if (remainingMins > 0) {
      return `${hours}h${remainingMins.toString().padStart(2, '0')}`;
    }
    return `${hours}h`;
  });

  ngOnInit(): void {
    this.loadTimeSlots();
  }

  loadTimeSlots(): void {
    this.isLoading.set(true);
    this.errorMsg.set(null);

    this.examTimeSlotApiService.getAllExamTimeSlots().subscribe({
      next: response => {
        const data = response.data || [];
        // Sort chronologically by start time
        data.sort((a, b) => {
          if (!a.startTime || !b.startTime) return 0;
          return a.startTime.localeCompare(b.startTime);
        });
        this.timeSlots.set(data);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement des créneaux d\'examens:', err);
        this.errorMsg.set('Impossible de charger les créneaux d\'examens pour le moment.');
        this.toastService.error('Impossible de charger les créneaux d\'examens.');
        this.isLoading.set(false);
      }
    });
  }

  formatTime(timeStr: string | undefined): string {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
      return `${parts[0]}:${parts[1]}`;
    }
    return timeStr;
  }

  getDuration(startTime: string | undefined, endTime: string | undefined): string {
    if (!startTime || !endTime) return '';
    const startParts = startTime.split(':').map(Number);
    const endParts = endTime.split(':').map(Number);
    if (startParts.length >= 2 && endParts.length >= 2) {
      const startMins = startParts[0] * 60 + startParts[1];
      const endMins = endParts[0] * 60 + endParts[1];
      const diff = endMins - startMins;
      if (diff > 0) {
        const hours = Math.floor(diff / 60);
        const mins = diff % 60;
        if (hours > 0) {
          return mins > 0 ? `${hours}h${mins}` : `${hours}h`;
        }
        return `${mins}m`;
      }
    }
    return '';
  }

  private getMinutes(timeStr: string): number {
    if (!timeStr) return 0;
    const parts = timeStr.split(':').map(Number);
    if (parts.length >= 2) {
      return parts[0] * 60 + parts[1];
    }
    return 0;
  }

  onSubmit(): void {
    if (this.slotForm.invalid || this.isSubmitting()) return;

    const val = this.slotForm.getRawValue();
    const code = val.code.trim().toUpperCase();
    const label = val.label.trim();
    const rawStart = val.startTime;
    const rawEnd = val.endTime;

    const startMins = this.getMinutes(rawStart);
    const endMins = this.getMinutes(rawEnd);

    if (endMins <= startMins) {
      this.toastService.error("L'heure de fin doit être supérieure à l'heure de début.");
      return;
    }

    this.isSubmitting.set(true);

    const payload: ExamTimeSlotRequest = {
      code: code,
      label: label,
      startTime: rawStart.length === 5 ? `${rawStart}:00` : rawStart,
      endTime: rawEnd.length === 5 ? `${rawEnd}:00` : rawEnd
    };

    this.examTimeSlotApiService.createExamTimeSlot(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.toastService.success('Créneau d\'examen ajouté avec succès !');
        this.slotForm.reset({
          code: '',
          label: '',
          startTime: '09:00',
          endTime: '12:00'
        });
        this.loadTimeSlots();
      },
      error: err => this.handleApiError(err)
    });
  }

  toggleActive(id: number | undefined): void {
    if (id === undefined) return;
    this.examTimeSlotApiService.toggleActiveStatus(id).subscribe({
      next: () => {
        this.toastService.success('Statut du créneau modifié avec succès !');
        this.loadTimeSlots();
      },
      error: err => {
        console.error('Erreur lors du basculement de l\'état:', err);
        this.toastService.error('Impossible de modifier le statut du créneau.');
      }
    });
  }

  private handleApiError(err: any): void {
    this.isSubmitting.set(false);
    console.error('Erreur API Créneaux d\'examens:', err);

    const msg = err.error?.message || err.error?.errorCode;
    let frenchMsg = 'Une erreur est survenue lors de la création du créneau.';

    // Translation of validation API errors
    if (msg === 'CODE_REQUIRED') {
      frenchMsg = 'Le code du créneau est obligatoire.';
    } else if (msg === 'LABEL_REQUIRED') {
      frenchMsg = 'Le libellé du créneau est obligatoire.';
    } else if (msg === 'START_TIME_REQUIRED') {
      frenchMsg = "L'heure de début est obligatoire.";
    } else if (msg === 'END_TIME_REQUIRED') {
      frenchMsg = "L'heure de fin est obligatoire.";
    } else if (msg === 'EXAM_TIME_SLOT_CODE_ALREADY_EXISTS' || msg === 'ENTITY_ALREADY_EXISTS') {
      frenchMsg = 'Ce code de créneau d\'examen existe déjà.';
    } else if (msg === 'START_TIME_MUST_BE_BEFORE_END_TIME') {
      frenchMsg = "L'heure de début doit être antérieure à l'heure de fin.";
    } else if (err.error?.errors && err.error.errors.length > 0) {
      const firstError = err.error.errors[0]?.message;
      if (firstError === 'CODE_REQUIRED') frenchMsg = 'Le code du créneau est obligatoire.';
      else if (firstError === 'LABEL_REQUIRED') frenchMsg = 'Le libellé du créneau est obligatoire.';
      else if (firstError === 'START_TIME_REQUIRED') frenchMsg = "L'heure de début est obligatoire.";
      else if (firstError === 'END_TIME_REQUIRED') frenchMsg = "L'heure de fin est obligatoire.";
      else if (firstError === 'START_TIME_MUST_BE_BEFORE_END_TIME') frenchMsg = "L'heure de début doit être antérieure à l'heure de fin.";
    }

    this.toastService.error(frenchMsg);
  }
}
