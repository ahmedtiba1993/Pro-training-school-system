import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TeacherControllerService } from '../../../../../core/api/api/teacher-controller.service';
import { TimetableSlotControllerService } from '../../../../../core/api/api/timetable-slot-controller.service';
import { TeacherSimpleResponse } from '../../../../../core/api/model/teacher-simple-response';
import { TimeSlotDefinitionResponse } from '../../../../../core/api/model/time-slot-definition-response';
import { TimetableSlotInfoResponse } from '../../../../../core/api/model/timetable-slot-info-response';
import { ToastService } from '../../../../../shared/services/toast.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-schedule-list-teacher',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './schedule-list-teacher.html',
  styleUrls: ['./schedule-list-teacher.css']
})
export class ScheduleListTeacher implements OnInit {
  private readonly teacherApiService = inject(TeacherControllerService);
  private readonly timetableApiService = inject(TimetableSlotControllerService);
  private readonly toastService = inject(ToastService);

  readonly days = [
    { code: 'MONDAY', label: 'Lundi' },
    { code: 'TUESDAY', label: 'Mardi' },
    { code: 'WEDNESDAY', label: 'Mercredi' },
    { code: 'THURSDAY', label: 'Jeudi' },
    { code: 'FRIDAY', label: 'Vendredi' },
    { code: 'SATURDAY', label: 'Samedi' }
  ];

  // --- SIGNALS ---
  readonly teachersList = signal<TeacherSimpleResponse[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isTimetableLoading = signal<boolean>(false);
  readonly isExporting = signal<boolean>(false);
  readonly selectedTeacherId = signal<number>(0);
  readonly searchQuery = signal<string>('');

  // --- DYNAMIC TIMETABLE API DATA ---
  readonly teacherName = signal<string>('');
  readonly timeSlotDefinitions = signal<TimeSlotDefinitionResponse[]>([]);
  readonly timetableSlots = signal<TimetableSlotInfoResponse[]>([]);

  // --- EFFECT: load the schedule whenever the selected teacher changes ---
  private readonly loadTimetableEffect = effect(() => {
    const teacherId = this.selectedTeacherId();
    if (teacherId > 0) {
      this.loadTimetableForTeacher(teacherId);
    }
  });

  ngOnInit(): void {
    this.loadTeachers();
  }

  loadTeachers(): void {
    this.isLoading.set(true);
    this.teacherApiService.getActiveTeachers().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.teachersList.set(res.data);
          if (res.data.length > 0) {
            this.selectedTeacherId.set(res.data[0].id || 0);
          }
        }
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement des enseignants:', err);
        this.toastService.error('Impossible de charger la liste des enseignants.');
        this.isLoading.set(false);
      }
    });
  }

  loadTimetableForTeacher(teacherId: number): void {
    this.isTimetableLoading.set(true);
    this.timetableSlots.set([]);
    this.timeSlotDefinitions.set([]);
    this.teacherName.set('');

    this.timetableApiService.getTimetableViewByTeacher(teacherId).subscribe({
      next: res => {
        if (res.success && res.data) {
          this.teacherName.set(res.data.teacherName || '');
          this.timeSlotDefinitions.set(res.data.timeSlotDefinitions || []);
          this.timetableSlots.set(res.data.timetableSlots || []);
        }
        this.isTimetableLoading.set(false);
      },
      error: err => {
        console.error("Erreur lors du chargement de l'emploi du temps:", err);
        this.toastService.error("Impossible de charger l'emploi du temps de cet enseignant.");
        this.isTimetableLoading.set(false);
      }
    });
  }

  readonly filteredTeachers = computed(() => {
    const query = this.searchQuery()
      .toLowerCase()
      .trim();
    const teachers = this.teachersList();
    if (!query) return teachers;
    return teachers.filter(
      t =>
        (t.firstName || '').toLowerCase().includes(query) ||
        (t.lastName || '').toLowerCase().includes(query) ||
        (t.code || '').toLowerCase().includes(query)
    );
  });

  readonly selectedTeacher = computed(() => {
    return this.teachersList().find(t => t.id === this.selectedTeacherId()) || null;
  });

  readonly sortedTimeSlotDefinitions = computed(() => {
    return [...this.timeSlotDefinitions()].sort(
      (a, b) => (a.orderIndex || 0) - (b.orderIndex || 0)
    );
  });

  readonly totalSlots = computed(() => this.timetableSlots().length);

  selectTeacher(id: number | undefined): void {
    if (id !== undefined) {
      this.selectedTeacherId.set(id);
    }
  }

  /**
   * Retrieves all slots for a given day and time slot.
   * Useful when there is WEEK_A / WEEK_B on the same time slot.
   */
  getSlotItems(dayOfWeek: string, timeSlotDefId: number | undefined): TimetableSlotInfoResponse[] {
    if (timeSlotDefId === undefined || timeSlotDefId === null) return [];
    return this.timetableSlots().filter(
      s => s.dayOfWeek === dayOfWeek && s.timeSlotDefinition?.id === timeSlotDefId
    );
  }

  getInitials(firstName: string | undefined, lastName: string | undefined): string {
    const first = firstName ? firstName.charAt(0) : '';
    const last = lastName ? lastName.charAt(0) : '';
    return (first + last).toUpperCase() || 'P';
  }

  getPeriodicityLabel(periodicity: string | undefined): string {
    switch (periodicity) {
      case 'WEEK_A':
        return 'Sem. A';
      case 'WEEK_B':
        return 'Sem. B';
      default:
        return '';
    }
  }

  getCardStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'bg-slate-50 border-slate-200';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'bg-indigo-50 border-indigo-100 hover:border-indigo-300';
    if (sub.includes('phys')) return 'bg-emerald-50 border-emerald-100 hover:border-emerald-300';
    if (sub.includes('math') || sub.includes('algè'))
      return 'bg-blue-50 border-blue-100 hover:border-blue-300';
    if (sub.includes('anglais') || sub.includes('commun'))
      return 'bg-purple-50 border-purple-100 hover:border-purple-300';
    if (sub.includes('base') || sub.includes('donn'))
      return 'bg-rose-50 border-rose-100 hover:border-rose-300';
    if (sub.includes('résea') || sub.includes('sécur'))
      return 'bg-teal-50 border-teal-100 hover:border-teal-300';
    if (sub.includes('java') || sub.includes('spring'))
      return 'bg-orange-50 border-orange-100 hover:border-orange-300';
    if (sub.includes('archi')) return 'bg-emerald-50 border-emerald-100 hover:border-emerald-300';
    return 'bg-slate-50 border-slate-200';
  }

  getTextStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'text-slate-900';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'text-indigo-900';
    if (sub.includes('phys')) return 'text-emerald-950';
    if (sub.includes('math') || sub.includes('algè')) return 'text-blue-900';
    if (sub.includes('anglais') || sub.includes('commun')) return 'text-purple-900';
    if (sub.includes('base') || sub.includes('donn')) return 'text-rose-950';
    if (sub.includes('résea') || sub.includes('sécur')) return 'text-teal-950';
    if (sub.includes('java') || sub.includes('spring')) return 'text-orange-900';
    if (sub.includes('archi')) return 'text-emerald-950';
    return 'text-slate-900';
  }

  getBadgeStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'text-slate-700';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'text-indigo-700';
    if (sub.includes('phys')) return 'text-emerald-700';
    if (sub.includes('math') || sub.includes('algè')) return 'text-blue-700';
    if (sub.includes('anglais') || sub.includes('commun')) return 'text-purple-700';
    if (sub.includes('base') || sub.includes('donn')) return 'text-rose-700';
    if (sub.includes('résea') || sub.includes('sécur')) return 'text-teal-700';
    if (sub.includes('java') || sub.includes('spring')) return 'text-orange-700';
    if (sub.includes('archi')) return 'text-emerald-700';
    return 'text-slate-700';
  }

  getBadgeBgStyle(subjectName: string | undefined): string {
    if (!subjectName) return 'bg-slate-100';
    const sub = subjectName.toLowerCase();
    if (sub.includes('algo')) return 'bg-indigo-100';
    if (sub.includes('phys')) return 'bg-emerald-100';
    if (sub.includes('math') || sub.includes('algè')) return 'bg-blue-100';
    if (sub.includes('anglais') || sub.includes('commun')) return 'bg-purple-100';
    if (sub.includes('base') || sub.includes('donn')) return 'bg-rose-100';
    if (sub.includes('résea') || sub.includes('sécur')) return 'bg-teal-100';
    if (sub.includes('java') || sub.includes('spring')) return 'bg-orange-100';
    if (sub.includes('archi')) return 'bg-emerald-100';
    return 'bg-slate-100';
  }

  getPeriodicityBadgeStyle(periodicity: string | undefined): string {
    switch (periodicity) {
      case 'WEEK_A':
        return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'WEEK_B':
        return 'bg-sky-100 text-sky-700 border-sky-200';
      default:
        return '';
    }
  }

  formatTime(timeStr: string | undefined): string {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
      return `${parts[0]}h${parts[1]}`;
    }
    return timeStr;
  }

  exportToPdf(teacherId: number): void {
    if (this.isExporting()) return;
    this.isExporting.set(true);

    this.timetableApiService
      .exportTimetablePdfByTeacher(teacherId, 'body', false, {
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
            link.download = `emploi_du_temps_enseignant_${teacherId}.pdf`;
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
