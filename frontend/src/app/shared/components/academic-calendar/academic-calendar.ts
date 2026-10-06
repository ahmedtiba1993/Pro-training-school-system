import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import {
  AcademicYearControllerService,
  AcademicCalendarResponse,
  CalendarPeriodResponse,
  CalendarHolidayResponse,
  CalendarSessionResponse,
} from '../../../core/api';

@Component({
  selector: 'app-academic-calendar',
  standalone: true,
  imports: [DatePipe, NgClass],
  templateUrl: './academic-calendar.html',
  host: {
    class: 'flex-1 flex flex-col min-h-0 overflow-hidden',
  },
})
export class AcademicCalendarComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearControllerService);

  readonly calendar = signal<AcademicCalendarResponse | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<'ALL' | 'PERIODS' | 'HOLIDAYS'>('ALL');

  readonly periodsCount = computed(() => this.calendar()?.periods?.length ?? 0);
  readonly holidaysCount = computed(() => this.calendar()?.holidays?.length ?? 0);
  readonly sessionsCount = computed(() => {
    const periods = this.calendar()?.periods ?? [];
    return periods.reduce((sum, p) => sum + (p.sessions?.length ?? 0), 0);
  });

  readonly sortedPeriods = computed(() => {
    const periods = this.calendar()?.periods ?? [];
    return [...periods].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  });

  readonly sortedHolidays = computed(() => {
    const holidays = this.calendar()?.holidays ?? [];
    return [...holidays].sort((a, b) => {
      const dateA = a.startDate ? new Date(a.startDate).getTime() : 0;
      const dateB = b.startDate ? new Date(b.startDate).getTime() : 0;
      return dateA - dateB;
    });
  });

  ngOnInit(): void {
    this.loadCalendar();
  }

  loadCalendar(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.academicYearService.getDefaultAcademicCalendar().subscribe({
      next: (response) => {
        if (response && response.success && response.data) {
          this.calendar.set(response.data);
        } else if (response && response.data) {
          this.calendar.set(response.data);
        } else {
          this.calendar.set(null);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement du calendrier scolaire:', err);
        this.calendar.set(null);
        this.error.set('Impossible de charger le calendrier de l\'année scolaire.');
        this.isLoading.set(false);
      },
    });
  }

  setTab(tab: 'ALL' | 'PERIODS' | 'HOLIDAYS'): void {
    this.activeTab.set(tab);
  }

  getHolidayTypeLabel(type: CalendarHolidayResponse.TypeEnum | undefined): string {
    switch (type) {
      case 'PUBLIC_HOLIDAY':
        return 'Jour Férié';
      case 'SCHOOL_BREAK':
        return 'Vacances Scolaires';
      case 'EXCEPTIONAL':
        return 'Congé Exceptionnel';
      default:
        return 'Congé';
    }
  }

  getHolidayBadgeClass(type: CalendarHolidayResponse.TypeEnum | undefined): string {
    switch (type) {
      case 'PUBLIC_HOLIDAY':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'SCHOOL_BREAK':
        return 'bg-teal-50 text-teal-700 border-teal-200';
      case 'EXCEPTIONAL':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }

  getSessionTypeLabel(type: CalendarSessionResponse.SessionTypeEnum | undefined): string {
    switch (type) {
      case 'MAIN':
        return 'Session Principale';
      case 'RETAKE':
        return 'Rattrapage';
      default:
        return type || 'Session d\'examen';
    }
  }

  getSessionBadgeClass(type: CalendarSessionResponse.SessionTypeEnum | undefined): string {
    switch (type) {
      case 'MAIN':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case 'RETAKE':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }
}
