import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { NgClass } from '@angular/common';
import { TimetableSlotControllerService } from '../../../../core/api/api/timetable-slot-controller.service';
import { TimeSlotDefinitionResponse } from '../../../../core/api/model/time-slot-definition-response';
import { TimetableSlotInfoResponse } from '../../../../core/api/model/timetable-slot-info-response';
import { TimetableSlotDetailResponse } from '../../../../core/api/model/timetable-slot-detail-response';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-teacher-schedules',
  standalone: true,
  imports: [NgClass],
  templateUrl: './schedules.html',
  styleUrl: './schedules.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class TeacherSchedules implements OnInit {
  private readonly timetableApiService = inject(TimetableSlotControllerService);

  readonly timeSlotDefinitions = signal<TimeSlotDefinitionResponse[]>([]);
  readonly timetableSlots = signal<TimetableSlotInfoResponse[]>([]);
  readonly isExporting = signal<boolean>(false);
  readonly isLoading = signal<boolean>(true);

  readonly selectedDayIndex = signal<number>(0);

  readonly days = [
    { code: 'MONDAY', label: 'Lundi', short: 'Lun' },
    { code: 'TUESDAY', label: 'Mardi', short: 'Mar' },
    { code: 'WEDNESDAY', label: 'Mercredi', short: 'Mer' },
    { code: 'THURSDAY', label: 'Jeudi', short: 'Jeu' },
    { code: 'FRIDAY', label: 'Vendredi', short: 'Ven' },
    { code: 'SATURDAY', label: 'Samedi', short: 'Sam' }
  ];

  readonly selectedDay = computed(() => this.days[this.selectedDayIndex()]);

  readonly isDetailModalOpen = signal<boolean>(false);
  readonly isDetailLoading = signal<boolean>(false);
  readonly activeSlotDetail = signal<TimetableSlotDetailResponse | null>(null);

  selectDay(index: number): void {
    this.selectedDayIndex.set(index);
  }

  formatTime(timeStr: string | undefined): string {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
      return `${parts[0]}h${parts[1]}`;
    }
    return timeStr;
  }

  getSlotItems(dayOfWeek: string, timeSlotDefId: number | undefined): TimetableSlotInfoResponse[] {
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
    return 'bg-slate-50 border-slate-250 hover:bg-slate-100/50';
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
    return 'text-slate-800';
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
    return 'text-slate-650';
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
    return 'bg-slate-150';
  }

  ngOnInit(): void {
    this.loadTimetableView();
  }

  loadTimetableView(): void {
    this.isLoading.set(true);
    this.timetableApiService.getMyTimetableView().subscribe({
      next: response => {
        if (response.success && response.data) {
          this.timeSlotDefinitions.set(response.data.timeSlotDefinitions || []);
          this.timetableSlots.set(response.data.timetableSlots || []);
        }
        this.isLoading.set(false);
      },
      error: err => {
        console.error("Erreur lors du chargement de l'emploi du temps du prof:", err);
        this.isLoading.set(false);
      }
    });
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
          this.closeDetailModal();
        }
        this.isDetailLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement du cours:', err);
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

  exportToPdf(): void {
    if (this.isExporting()) return;
    this.isExporting.set(true);

    this.timetableApiService
      .exportMyTimetablePdf('body', false, {
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
            link.download = `mon_emploi_du_temps.pdf`;
            link.click();
            window.URL.revokeObjectURL(url);
          } catch (e) {
            console.error('Error processing PDF blob:', e);
          }
        },
        error: err => {
          console.error("Erreur lors de l'export PDF:", err);
        }
      });
  }
}
