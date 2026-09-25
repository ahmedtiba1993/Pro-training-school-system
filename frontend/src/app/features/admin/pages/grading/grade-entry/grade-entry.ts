import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import {
  PromotionControllerService,
  PromotionLookupResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-grade-entry',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './grade-entry.html',
  styleUrl: './grade-entry.css'
})
export class GradeEntry implements OnInit {
  // --- DEPENDENCY INJECTIONS ---
  private readonly promotionService = inject(PromotionControllerService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  // --- STATE SIGNALS ---
  public readonly promotions = signal<PromotionLookupResponse[]>([]);
  public readonly isLoading = signal<boolean>(true);
  public readonly searchQuery = signal<string>('');

  // --- COMPUTED & FILTER SIGNALS ---
  public readonly filteredPromotions = computed(() => {
    const list = this.promotions();
    const query = this.searchQuery().toLowerCase().trim();

    if (!query) {
      return list;
    }

    return list.filter(promo => promo.name?.toLowerCase().includes(query));
  });

  public ngOnInit(): void {
    this.loadActivePromotions();
  }

  /**
   * Loads the list of active promotions via the OpenAPI service.
   */
  public loadActivePromotions(): void {
    this.isLoading.set(true);
    this.promotionService.getActivePromotionsLookup()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          if (response?.success && response.data) {
            this.promotions.set(response.data);
          } else {
            this.toastService.error('Impossible de charger la liste des promotions actives.');
          }
        },
        error: (err) => {
          console.error('Erreur lors du chargement des promotions actives pour la saisie des notes', err);
          this.toastService.error('Une erreur est survenue lors de la communication avec le serveur.');
        }
      });
  }

  /**
   * Selects a promotion to display its list of assessments.
   */
  public selectPromotion(promo: PromotionLookupResponse): void {
    if (!promo?.id) return;
    this.router.navigate(['/admin/grades', promo.id, 'assessments']);
  }
}
