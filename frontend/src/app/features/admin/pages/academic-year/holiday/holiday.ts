import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AcademicYearControllerService,
  HolidayControllerService,
  HolidayRequest,
  AcademicYearResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-holiday',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './holiday.html',
  styleUrl: './holiday.css'
})
export class Holiday implements OnInit {
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);
  private academicYearApi = inject(AcademicYearControllerService);
  private holidayApi = inject(HolidayControllerService); // --- GLOBAL STATES ---

  academicYears = signal<AcademicYearResponse[]>([]);
  selectedYearId = signal<number | null>(null);
  holidays = signal<any[]>([]); // List of holidays for the selected year

  isLoading = signal<boolean>(true);
  isSaving = signal<boolean>(false); // --- MODAL STATES ---

  showHolidayModal = signal<boolean>(false);
  editingHolidayId = signal<number | null>(null); // --- FORM ---

  holidayForm = this.fb.group({
    title: ['', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['']
  }); // --- REACTIVE STATISTICS ---

  totalHolidayDays = computed(() => {
    return this.holidays().reduce((total, h) => total + (h.numberOfDays || 0), 0);
  });

  nextHoliday = computed(() => {
    const today = new Date().getTime();
    const upcoming = this.holidays()
      .filter(h => new Date(h.startDate).getTime() >= today)
      .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime());

    return upcoming.length > 0 ? upcoming[0] : null;
  });

  daysUntilNextHoliday = computed(() => {
    const next = this.nextHoliday();
    if (!next) return null;

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const nextDate = new Date(next.startDate);

    const diffTime = nextDate.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  });

  ngOnInit() {
    this.loadActiveOrPlannedYears();
  } // ========================================== // DATA LOADING // ==========================================

  loadActiveOrPlannedYears() {
    this.isLoading.set(true); // Using the getAllAcademicYears method from the generated Swagger
    this.academicYearApi.getActiveOrPlannedYears().subscribe({
      next: (res: any) => {
        const years = res.data?.content || res.data || [];
        this.academicYears.set(years); // Selects the active year by default, or the first one in the list

        const active = years.find((y: any) => y.isActive) || years[0];
        if (active) {
          this.selectedYearId.set(active.id);
          this.loadHolidaysForYear(active.id);
        } else {
          this.isLoading.set(false);
        }
      },
      error: () => {
        this.toast.error('Erreur lors du chargement des années scolaires.');
        this.isLoading.set(false);
      }
    });
  }

  onYearChange(event: Event) {
    const yearId = Number((event.target as HTMLSelectElement).value);
    this.selectedYearId.set(yearId);
    this.loadHolidaysForYear(yearId);
  }

  loadHolidaysForYear(yearId: number) {
    this.isLoading.set(true);
    this.holidayApi.getHolidaysByAcademicYearId(yearId).subscribe({
      next: (res: any) => {
        this.holidays.set(res.data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.toast.error('Impossible de charger le calendrier des vacances.');
        this.holidays.set([]);
        this.isLoading.set(false);
      }
    });
  } // ========================================== // MODAL ACTIONS (Add & Edit) // ==========================================

  openModal() {
    this.editingHolidayId.set(null);
    this.holidayForm.reset();
    this.showHolidayModal.set(true);
  }

  editHoliday(holiday: any) {
    this.editingHolidayId.set(holiday.id);

    this.holidayForm.patchValue({
      title: holiday.title,
      startDate: this.formatDateForInput(holiday.startDate),
      endDate: this.formatDateForInput(holiday.endDate)
    });

    this.showHolidayModal.set(true);
  }

  closeModal() {
    this.showHolidayModal.set(false);
    this.editingHolidayId.set(null);
  }

  submitHoliday() {
    if (this.holidayForm.invalid) {
      this.holidayForm.markAllAsTouched();
      return;
    }

    const yearId = this.selectedYearId();
    if (!yearId) return;

    this.isSaving.set(true);

    const payload: HolidayRequest = {
      title: this.holidayForm.value.title!,
      startDate: this.holidayForm.value.startDate! as any,
      endDate: this.holidayForm.value.endDate! as any,
      academicYearId: yearId
    };

    const holidayId = this.editingHolidayId();

    if (holidayId) {
      // UPDATE MODE: Ensure you have updateHoliday on the Backend
      this.holidayApi.updateHoliday(holidayId, payload).subscribe({
        next: () => {
          this.toast.success('Vacances modifiées avec succès');
          this.isSaving.set(false);
          this.closeModal();
          this.loadHolidaysForYear(yearId);
        },
        error: err => this.handleBackendError(err)
      });
    } else {
      // CREATE MODE
      this.holidayApi.createHoliday(payload).subscribe({
        next: () => {
          this.toast.success('Vacances ajoutées avec succès');
          this.isSaving.set(false);
          this.closeModal();
          this.loadHolidaysForYear(yearId);
        },
        error: err => this.handleBackendError(err)
      });
    }
  } // ========================================== // UTILITIES & ERROR HANDLING // ==========================================

  private handleBackendError(err: any) {
    this.isSaving.set(false);
    const errorBody = err?.error;

    if (errorBody?.errorCode === 'ENTITY_ALREADY_EXISTS') {
      this.toast.error('Une vacance avec ce nom existe déjà pour cette année.');
    } else if (errorBody?.message === 'END_DATE_MUST_BE_AFTER_START_DATE') {
      this.toast.error('La date de fin doit être strictement après la date de début.');
    } else {
      this.toast.error("Erreur lors de l'enregistrement.");
    }
  }

  private formatDateForInput(dateValue: any): string {
    if (!dateValue) return '';
    if (typeof dateValue === 'string' && dateValue.length === 10) return dateValue;
    const d = new Date(dateValue);
    if (isNaN(d.getTime())) return '';
    return d.toISOString().split('T')[0];
  }
}
