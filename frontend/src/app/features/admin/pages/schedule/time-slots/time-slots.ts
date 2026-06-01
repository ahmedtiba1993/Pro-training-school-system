import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { TimeSlotDefinitionControllerService } from '../../../../../core/api/api/time-slot-definition-controller.service';
import { TimeSlotDefinitionResponse } from '../../../../../core/api/model/time-slot-definition-response';
import { TimeSlotDefinitionRequest } from '../../../../../core/api/model/time-slot-definition-request';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-time-slots',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './time-slots.html',
  styleUrl: './time-slots.css'
})
export class TimeSlots implements OnInit {
  private readonly timeSlotApiService = inject(TimeSlotDefinitionControllerService);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly timeSlots = signal<TimeSlotDefinitionResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);

  readonly editingSlotId = signal<number | null>(null);
  readonly isEditMode = computed(() => this.editingSlotId() !== null);

  readonly slotForm = this.fb.nonNullable.group({
    label: ['', [Validators.required, Validators.minLength(2)]],
    startTime: ['08:30', [Validators.required]],
    endTime: ['10:00', [Validators.required]]
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

    this.timeSlotApiService.getAllTimeSlots().subscribe({
      next: response => {
        const data = response.data || [];
        data.sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
        this.timeSlots.set(data);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement des créneaux:', err);
        this.errorMsg.set('Impossible de charger les créneaux horaires pour le moment.');
        this.toastService.error('Impossible de charger les créneaux horaires.');
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

    // Détermination du code (Ex: "Séance 6 (S6)" -> "S6", "Pause Midi" -> "PAUSE_MIDI")
    let code = label.toUpperCase().replace(/[^A-Z0-9_]/g, '_');
    const parenMatch = label.match(/\(([^)]+)\)/);
    if (parenMatch) {
      code = parenMatch[1].trim().toUpperCase().replace(/[^A-Z0-9_]/g, '_');
    }
    if (!code) {
      code = 'SLOT_' + Date.now();
    }

    // Calcul de l'index d'ordre chronologique
    let orderIndex = 1;
    for (const slot of this.timeSlots()) {
      if (slot.id !== this.editingSlotId() && slot.startTime && this.getMinutes(slot.startTime) < startMins) {
        orderIndex++;
      }
    }

    const payload: TimeSlotDefinitionRequest = {
      code: code.substring(0, 50),
      label: label,
      startTime: rawStart.length === 5 ? `${rawStart}:00` : rawStart,
      endTime: rawEnd.length === 5 ? `${rawEnd}:00` : rawEnd,
      orderIndex: orderIndex
    };

    if (this.isEditMode()) {
      const id = this.editingSlotId();
      if (id === null) return;
      this.timeSlotApiService.updateTimeSlot(id, payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.toastService.success('Créneau horaire modifié avec succès !');
          this.cancelEdit();
          this.loadTimeSlots();
        },
        error: err => this.handleApiError(err)
      });
    } else {
      this.timeSlotApiService.createTimeSlot(payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.toastService.success('Créneau horaire ajouté avec succès !');
          this.slotForm.reset({
            label: '',
            startTime: '08:30',
            endTime: '10:00'
          });
          this.loadTimeSlots();
        },
        error: err => this.handleApiError(err)
      });
    }
  }

  startEdit(slot: TimeSlotDefinitionResponse): void {
    this.editingSlotId.set(slot.id ?? null);
    this.slotForm.patchValue({
      label: slot.label ?? '',
      startTime: this.formatTime(slot.startTime),
      endTime: this.formatTime(slot.endTime)
    });
  }

  cancelEdit(): void {
    this.editingSlotId.set(null);
    this.resetForm();
  }

  resetForm(): void {
    this.slotForm.reset({
      label: '',
      startTime: '08:30',
      endTime: '10:00'
    });
  }

  private handleApiError(err: any): void {
    this.isSubmitting.set(false);
    console.error('Erreur API Créneaux:', err);

    const msg = err.error?.message || err.error?.errorCode;
    let frenchMsg = 'Une erreur est survenue lors du traitement du créneau.';

    // Traduction des messages de validation d'API
    if (msg === 'CODE_REQUIRED') {
      frenchMsg = 'Le code du créneau est obligatoire.';
    } else if (msg === 'LABEL_REQUIRED') {
      frenchMsg = 'Le libellé du créneau est obligatoire.';
    } else if (msg === 'START_TIME_REQUIRED') {
      frenchMsg = "L'heure de début est obligatoire.";
    } else if (msg === 'END_TIME_REQUIRED') {
      frenchMsg = "L'heure de fin est obligatoire.";
    } else if (msg === 'ORDER_INDEX_REQUIRED') {
      frenchMsg = "L'index d'ordre est obligatoire.";
    } 
    // Traduction des contraintes de base de données et cas métiers
    else if (msg === 'TIME_SLOT_ALREADY_EXISTS' || msg === 'TIME_SLOT_CODE_ALREADY_EXISTS' || err.error?.errorCode === 'ENTITY_ALREADY_EXISTS') {
      frenchMsg = 'Ce créneau horaire ou ce code existe déjà.';
    } else if (
      msg === 'TIME_SLOT_OVERLAP' || 
      msg === 'TIME_SLOTS_OVERLAP' || 
      msg === 'TIME_SLOT_CONFLICT' || 
      msg === 'TIME_SLOT_OVERLAP_DETECTED'
    ) {
      frenchMsg = 'Un chevauchement de créneau horaire a été détecté.';
    } else if (msg === 'TIME_SLOT_INVALID_RANGE') {
      frenchMsg = "La plage horaire saisie n'est pas valide.";
    } else if (msg === 'TIME_SLOT_NOT_FOUND') {
      frenchMsg = 'Le créneau horaire à modifier est introuvable.';
    } else if (err.error?.errors && err.error.errors.length > 0) {
      // Gestion des erreurs dans la liste détaillée si existante
      const firstError = err.error.errors[0]?.message;
      if (firstError === 'CODE_REQUIRED') frenchMsg = 'Le code du créneau est obligatoire.';
      else if (firstError === 'LABEL_REQUIRED') frenchMsg = 'Le libellé du créneau est obligatoire.';
      else if (firstError === 'START_TIME_REQUIRED') frenchMsg = "L'heure de début est obligatoire.";
      else if (firstError === 'END_TIME_REQUIRED') frenchMsg = "L'heure de fin est obligatoire.";
      else if (firstError === 'TIME_SLOT_OVERLAP_DETECTED') frenchMsg = 'Un chevauchement de créneau horaire a été détecté.';
      else if (firstError === 'TIME_SLOT_NOT_FOUND') frenchMsg = 'Le créneau horaire à modifier est introuvable.';
    }

    this.toastService.error(frenchMsg);
  }
}
