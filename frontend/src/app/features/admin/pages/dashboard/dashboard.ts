import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DashboardControllerService, EnrollmentStatsResponse } from '../../../../core/api';
import { Header } from '../../../../layout/admin-layout/header/header';

@Component({
  selector: 'app-dashboard',
  imports: [Header],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private dashboardService = inject(DashboardControllerService);

  stats = signal<EnrollmentStatsResponse | null>(null);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  totalCount = computed(() => this.stats()?.totalCount ?? 0);
  maleCount = computed(() => this.stats()?.maleCount ?? 0);
  femaleCount = computed(() => this.stats()?.femaleCount ?? 0);

  totalGender = computed(() => this.maleCount() + this.femaleCount());

  malePercentage = computed(() => {
    const total = this.totalGender();
    return total > 0 ? Math.round((this.maleCount() / total) * 100) : 0;
  });

  femalePercentage = computed(() => {
    const total = this.totalGender();
    return total > 0 ? Math.round((this.femaleCount() / total) * 100) : 0;
  });

  // SVG parameters for donut chart (r = 40, cx = 50, cy = 50)
  readonly r = 40;
  readonly circ = 2 * Math.PI * this.r; // ~251.327

  femaleStrokeOffset = computed(() => {
    const pct = this.femalePercentage();
    return this.circ - (pct / 100) * this.circ;
  });

  maleStrokeOffset = computed(() => {
    const pct = this.malePercentage();
    return this.circ - (pct / 100) * this.circ;
  });

  maleRotation = computed(() => {
    // Start rotating male segment from where female segment ends
    return -90 + (this.femalePercentage() / 100) * 360;
  });

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading.set(true);
    this.error.set(null);
    this.dashboardService.getAdminEnrollmentStats().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.stats.set(res.data);
        } else {
          this.error.set(res.message || 'Impossible de charger les statistiques.');
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error fetching dashboard stats:', err);
        this.error.set('Une erreur est survenue lors de la communication avec le serveur.');
        this.loading.set(false);
      }
    });
  }
}

