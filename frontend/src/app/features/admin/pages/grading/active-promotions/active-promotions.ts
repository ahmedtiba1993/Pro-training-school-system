import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import {
  PromotionControllerService,
  PromotionLookupResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-active-active-promotions',
  standalone: true,
  imports: [],
  templateUrl: './active-promotions.html',
  styleUrl: './active-promotions.css'
})
export class ActivePromotions implements OnInit {
  // --- DEPENDENCY INJECTIONS ---
  private readonly promotionService = inject(PromotionControllerService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  // --- STATE SIGNALS ---
  public readonly promotions = signal<PromotionLookupResponse[]>([]);
  public readonly isLoading = signal<boolean>(true);
  public readonly searchQuery = signal<string>('');
  public readonly isNavigatingId = signal<number | null>(null);

  // --- FILTER COMPUTED SIGNALS ---
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
   * Loads the list of active promotions via the lookup API.
   */
  private loadActivePromotions(): void {
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
          console.error('Error loading active promotions', err);
          this.toastService.error('Une erreur est survenue lors de la communication avec le serveur.');
        }
      });
  }

  /**
   * Navigates the user to the subjects display page for the selected promotion.
   */
  public manageSubjects(id: number | undefined): void {
    if (id === undefined || id === null) return;
    this.router.navigate([`/admin/grading/${id}/subjects`]);
  }
}

