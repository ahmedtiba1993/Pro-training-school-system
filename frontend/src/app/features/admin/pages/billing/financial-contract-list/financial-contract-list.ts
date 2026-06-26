import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../../../../shared/services/toast.service';
import {
  FinancialContractControllerService,
  FinancialContractListResponse,
  FinancialContractSearchRequest,
  PromotionControllerService,
  PromotionLookupResponse,
  ActivateContractRequest
} from '../../../../../core/api';

@Component({
  selector: 'app-financial-contract-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './financial-contract-list.html',
  styleUrl: './financial-contract-list.css'
})
export class FinancialContractList implements OnInit {
  private readonly financialContractService = inject(FinancialContractControllerService);
  private readonly promotionService = inject(PromotionControllerService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // --- STATE (Activation Modal) ---
  isActivateModalOpen = signal<boolean>(false);
  isConfirmModalOpen = signal<boolean>(false);
  selectedContractForActivation = signal<FinancialContractListResponse | null>(null);
  isSubmitting = signal<boolean>(false);

  activationForm = this.fb.nonNullable.group({
    discountAmount: [0, [Validators.required, Validators.min(0)]],
    paymentMethod: ['CASH', [Validators.required]],
    paymentDate: ['', [Validators.required]],
    reference: ['']
  });

  // --- STATE (Table Data & Loading) ---
  contracts = signal<FinancialContractListResponse[]>([]);
  isLoading = signal<boolean>(true);

  // --- STATE (Pagination) ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // --- STATE (Filters) ---
  searchTerm = signal<string>('');
  statusFilter = signal<string>('ALL');
  promotionFilterId = signal<number | null>(null);
  segmentedFilter = signal<string>('GLOBAL'); // GLOBAL, HOMOLOGUE, CONTINUE, ACCELERE

  // --- LOOKUP DATA ---
  promotions = signal<PromotionLookupResponse[]>([]);

  // --- STATE (Statistics) ---
  activeRevenue = signal<number | null>(null);
  totalCollected = signal<number | null>(null);
  remainingDebt = signal<number | null>(null);
  losses = signal<number | null>(null);
  pendingPayments = signal<number | null>(null);
  isStatsLoading = signal<boolean>(true);

  // --- LIFECYCLE ---
  ngOnInit(): void {
    this.loadActiveRevenue();
    this.loadPromotions();
    this.loadContracts();
    this.setupFormValidation();
  }

  // --- API CALLS ---
  loadActiveRevenue(): void {
    this.isStatsLoading.set(true);
    let mappedTrainingType: 'CONTINUOUS' | 'ACCELERATED' | 'ACCREDITED' | undefined = undefined;
    const segment = this.segmentedFilter().toUpperCase();
    if (segment === 'HOMOLOGUE') {
      mappedTrainingType = 'ACCREDITED';
    } else if (segment === 'CONTINUE') {
      mappedTrainingType = 'CONTINUOUS';
    } else if (segment === 'ACCELERE') {
      mappedTrainingType = 'ACCELERATED';
    }

    this.financialContractService.getActiveRevenueStat(mappedTrainingType).subscribe({
      next: response => {
        if (response && response.success && response.data) {
          const data = response.data;
          this.activeRevenue.set(data.activeRevenue ?? null);
          this.totalCollected.set(data.totalCollected ?? null);
          this.remainingDebt.set(data.remainingDebt ?? null);
          this.losses.set(data.losses ?? null);
          this.pendingPayments.set(data.pendingPayments ?? null);
        }
        this.isStatsLoading.set(false);
      },
      error: err => {
        console.error('Error fetching active revenue stats:', err);
        this.isStatsLoading.set(false);
      }
    });
  }

  loadPromotions(): void {
    this.promotionService.getActivePromotionsLookup().subscribe({
      next: response => {
        if (response && response.success && response.data) {
          this.promotions.set(response.data || []);
        }
      },
      error: err => {
        console.error('Error loading promotions lookup:', err);
        this.toastService.error('Erreur lors du chargement des promotions');
      }
    });
  }

  loadContracts(): void {
    this.isLoading.set(true);

    // Map UI status filter options to backend ContractStatus enum values
    let mappedStatus: FinancialContractSearchRequest.StatusEnum | undefined = undefined;
    const filter = this.statusFilter();
    if (filter === 'OVERDUE') {
      mappedStatus = 'SETTLED_WITH_DEBT';
    } else if (filter === 'PAID') {
      mappedStatus = 'FULLY_PAID';
    } else if (filter === 'DROPPED_OUT') {
      mappedStatus = 'CANCELLED';
    } else if (filter === 'ACTIVE') {
      mappedStatus = 'ACTIVE';
    } else if (filter !== 'ALL') {
      mappedStatus = filter as FinancialContractSearchRequest.StatusEnum;
    }

    // Map UI segmented filter to backend TrainingType enum
    let mappedTrainingType: FinancialContractSearchRequest.TrainingTypeEnum | undefined = undefined;
    const segment = this.segmentedFilter().toUpperCase();
    if (segment === 'HOMOLOGUE') {
      mappedTrainingType = 'ACCREDITED';
    } else if (segment === 'CONTINUE') {
      mappedTrainingType = 'CONTINUOUS';
    } else if (segment === 'ACCELERE') {
      mappedTrainingType = 'ACCELERATED';
    }

    const searchRequest: FinancialContractSearchRequest = {
      keyword: this.searchTerm().trim() || undefined,
      status: mappedStatus,
      promotionId: this.promotionFilterId() || undefined,
      trainingType: mappedTrainingType,
      page: this.currentPage(),
      size: this.pageSize()
    };

    this.financialContractService.getAllFinancialContractsPaged(searchRequest).subscribe({
      next: response => {
        if (response && response.success && response.data) {
          const pageData = response.data;
          this.contracts.set(pageData.content || []);
          this.totalElements.set(pageData.totalElements || 0);
          this.totalPages.set(pageData.totalPages || 0);
        } else {
          this.contracts.set([]);
          this.totalElements.set(0);
          this.totalPages.set(0);
        }
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error loading financial contracts:', err);
        this.toastService.error('Erreur lors du chargement des contrats financiers');
        this.contracts.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.isLoading.set(false);
      }
    });
  }

  // --- COMPUTED STATES (Client-Side Filters & Stats over current page list) ---

  // Post-filter to handle client-side refinements
  filteredContracts = computed(() => {
    let list = this.contracts();

    // Secondary client-side refinement for overdue if selected
    if (this.statusFilter() === 'OVERDUE') {
      list = list.filter(c => (c.remainingDebt || 0) > 0);
    }

    return list;
  });

  // Pagination bounds indicators
  startElement = computed(() => {
    if (this.filteredContracts().length === 0) return 0;
    return this.currentPage() * this.pageSize() + 1;
  });

  endElement = computed(() => {
    const count = this.filteredContracts().length;
    return this.currentPage() * this.pageSize() + count;
  });

  // --- ACTIONS & HANDLERS ---
  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  applySearch(): void {
    this.currentPage.set(0);
    this.loadContracts();
  }

  onStatusChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.statusFilter.set(select.value);
  }

  onPromotionChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const value = select.value;
    this.promotionFilterId.set(value === 'ALL' ? null : Number(value));
  }

  setSegmentedFilter(filterType: string): void {
    this.segmentedFilter.set(filterType);
    this.loadActiveRevenue();
    this.applySearch();
  }

  onPageSizeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.pageSize.set(Number(select.value));
    this.currentPage.set(0);
    this.loadContracts();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadContracts();
    }
  }

  // --- UI HELPERS ---
  getInitials(firstName?: string, lastName?: string): string {
    const first = firstName ? firstName.charAt(0).toUpperCase() : '';
    const last = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${first}${last}` || '??';
  }

  getStatusBadgeClasses(contract: FinancialContractListResponse): string {
    const status = contract.status;
    if (status === 'FULLY_PAID') {
      return 'bg-emerald-50 text-emerald-700 border-emerald-100';
    }
    if (status === 'CANCELLED') {
      return 'bg-slate-100 text-slate-800 border-slate-200';
    }
    if (status === 'SETTLED_WITH_DEBT') {
      return 'bg-rose-50 text-rose-700 border-rose-100 animate-pulse';
    }
    if (status === 'DRAFT') {
      return 'bg-slate-150 text-slate-600 border-slate-200';
    }
    return 'bg-blue-50 text-blue-700 border-blue-100';
  }

  getStatusLabel(contract: FinancialContractListResponse): string {
    const status = contract.status;
    if (status === 'FULLY_PAID') {
      return 'Soldé';
    }
    if (status === 'CANCELLED') {
      return 'Abandon / Annulé';
    }
    if (status === 'SETTLED_WITH_DEBT') {
      return 'Dette active';
    }
    if (status === 'DRAFT') {
      return 'Brouillon';
    }
    return 'En cours';
  }

  // --- CONTRACT ACTIVATION METHODS ---
  private setupFormValidation(): void {
    this.activationForm.get('paymentMethod')?.valueChanges.subscribe(method => {
      const refControl = this.activationForm.get('reference');
      if (method === 'CHECK' || method === 'BANK_TRANSFER') {
        refControl?.setValidators([Validators.required]);
      } else {
        refControl?.clearValidators();
      }
      refControl?.updateValueAndValidity();
    });
  }

  private getLocalDateTimeString(): string {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - offset).toISOString().slice(0, 16);
  }

  openActivateModal(contract: FinancialContractListResponse): void {
    this.selectedContractForActivation.set(contract);
    this.activationForm.reset({
      discountAmount: contract.discountAmount || 0,
      paymentMethod: 'CASH',
      paymentDate: this.getLocalDateTimeString(),
      reference: ''
    });
    this.isActivateModalOpen.set(false); // Make sure it's reset
    this.isConfirmModalOpen.set(false);
    this.isActivateModalOpen.set(true);
  }

  closeActivateModal(): void {
    this.isActivateModalOpen.set(false);
    this.selectedContractForActivation.set(null);
  }

  openConfirmModal(): void {
    if (this.activationForm.invalid || this.isSubmitting()) {
      this.activationForm.markAllAsTouched();
      return;
    }
    this.isConfirmModalOpen.set(true);
  }

  closeConfirmModal(): void {
    this.isConfirmModalOpen.set(false);
  }

  getFriendlyPaymentMethod(method?: string): string {
    if (method === 'CASH') return 'Espèces';
    if (method === 'CHECK') return 'Chèque';
    if (method === 'BANK_TRANSFER') return 'Virement Bancaire';
    if (method === 'CREDIT_CARD') return 'Carte Bancaire';
    if (method === 'ONLINE_STRIPE') return 'Stripe (En ligne)';
    return method || '';
  }

  submitActivation(): void {
    const contract = this.selectedContractForActivation();
    if (!contract || !contract.id || this.activationForm.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.activationForm.getRawValue();

    // Format paymentDate: append ':00' to form value if it has length 16 (YYYY-MM-DDTHH:mm)
    let paymentDateStr = formValue.paymentDate;
    if (paymentDateStr && paymentDateStr.length === 16) {
      paymentDateStr += ':00';
    }

    const requestPayload: ActivateContractRequest = {
      discountAmount: formValue.discountAmount,
      paymentMethod: formValue.paymentMethod as ActivateContractRequest.PaymentMethodEnum,
      paymentDate: paymentDateStr,
      reference: formValue.reference || undefined
    };

    this.financialContractService.activateContract(contract.id, requestPayload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.toastService.success('Le contrat a été activé avec succès.');
        this.closeConfirmModal();
        this.closeActivateModal();
        this.loadActiveRevenue();
        this.loadContracts();
      },
      error: err => {
        this.isSubmitting.set(false);
        this.closeConfirmModal();
        console.error('Error activating contract:', err);
        const backendMessage = err.error?.message;
        if (backendMessage) {
          this.toastService.error(`Erreur d'activation : ${backendMessage}`);
        } else {
          this.toastService.error("Impossible d'activer le contrat.");
        }
      }
    });
  }

  viewContractTransactions(contract: FinancialContractListResponse): void {
    this.router.navigate(['/admin/billing/contracts', contract.id, 'transactions'], {
      state: { contract }
    });
  }
}
