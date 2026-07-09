import { Component, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ScheduleControllerService } from '../../../../core/api/api/schedule-controller.service';
import { ScheduleResponse } from '../../../../core/api/model/schedule-response';

@Component({
  selector: 'app-student-schedules',
  standalone: true,
  imports: [NgClass, RouterLink],
  templateUrl: './schedules.html',
  styleUrl: './schedules.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class StudentSchedules implements OnInit {
  private scheduleService = inject(ScheduleControllerService);

  schedules = signal<ScheduleResponse[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.fetchSchedules();
  }

  fetchSchedules(): void {
    this.isLoading.set(true);
    this.scheduleService.getMySchedules().subscribe({
      next: (response) => {
        if (response.data) {
          this.schedules.set(response.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching student schedules:', err);
        this.isLoading.set(false);
      }
    });
  }

  // --- UI Helpers ---
  getStatusConfig(status?: string) {
    switch (status) {
      case 'ACTIVE':
        return { label: 'Actif', classes: 'bg-emerald-100 text-emerald-700 border-emerald-250' };
      case 'DRAFT':
        return { label: 'Brouillon', classes: 'bg-amber-100 text-amber-700 border-amber-250' };
      case 'ARCHIVED':
        return { label: 'Archivé', classes: 'bg-slate-100 text-slate-650 border-slate-250' };
      default:
        return {
          label: status || 'Inconnu',
          classes: 'bg-slate-50 text-slate-500 border-slate-200'
        };
    }
  }
}
