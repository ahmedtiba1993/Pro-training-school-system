import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { catchError, map, switchMap, of } from 'rxjs';
import {
  PaymentTransactionControllerService,
  FinancialContractControllerService,
  PaymentTransactionRequest,
  PaymentTransactionResponse,
  FinancialContractListResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-payment-transaction',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './payment-transaction.html',
  styleUrl: './payment-transaction.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentTransaction implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly paymentTransactionService = inject(PaymentTransactionControllerService);
  private readonly financialContractService = inject(FinancialContractControllerService);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  // --- STATE (Signals) ---
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);

  // UI States
  isPaymentModalOpen = signal<boolean>(false);
  isInstallmentModalOpen = signal<boolean>(false);
  isConfirmModalOpen = signal<boolean>(false);
  isStatusModalOpen = signal<boolean>(false);
  isConfirmStatusModalOpen = signal<boolean>(false);
  selectedTransaction = signal<PaymentTransactionResponse | null>(null);
  selectedNewStatus = signal<PaymentTransactionResponse.StatusEnum | ''>('');

  // Data States
  contractId!: number;
  contract = signal<FinancialContractListResponse | null>(null);
  transactions = signal<PaymentTransactionResponse[]>([]);

  // --- FORM MODELS ---
  selectedInstallmentId = 'contract';
  paymentForm!: FormGroup;
  installmentForm!: FormGroup;

  // --- COMPUTED SIGNALS (Replaces Getters, ultra-performing because cached) ---
  totalAmount = computed(() => this.contract()?.netToPay || 0);

  paidAmount = computed(() => {
    const valid = this.transactions().filter(t => t.status === 'CLEARED');
    return valid.reduce((sum, t) => sum + (t.amount || 0), 0);
  });

  pendingAmount = computed(() => {
    const pending = this.transactions().filter(t => t.status === 'PENDING');
    return pending.reduce((sum, t) => sum + (t.amount || 0), 0);
  });

  remainingDebt = computed(() => {
    const debt = this.totalAmount() - this.paidAmount();
    return debt > 0 ? debt : 0;
  });

  recoveryRate = computed(() => {
    const total = this.totalAmount();
    return total > 0 ? Math.round((this.paidAmount() / total) * 100) : 0;
  });

  ngOnInit(): void {
    this.initForms();

    this.route.paramMap.subscribe(params => {
      const idStr = params.get('id');
      if (idStr) {
        this.contractId = Number(idStr);

        // Optimized initialization via router state
        const stateContract = history.state?.contract;
        if (stateContract && stateContract.id === this.contractId) {
          this.contract.set(stateContract);
        }

        this.loadData();
      }
    });
  }

  private initForms(): void {
    this.paymentForm = this.fb.group({
      amount: [0, [Validators.required, Validators.min(0.01)]],
      paymentMethod: ['CASH', [Validators.required]],
      reference: ['']
    });

    // Cross-validation dynamique
    this.paymentForm.get('paymentMethod')?.valueChanges.subscribe(method => {
      const refControl = this.paymentForm.get('reference');
      if (method === 'CHECK' || method === 'BANK_TRANSFER') {
        refControl?.setValidators([Validators.required]);
      } else {
        refControl?.clearValidators();
      }
      refControl?.updateValueAndValidity();
    });

    this.installmentForm = this.fb.group({
      name: ['', [Validators.required]],
      amount: [0, [Validators.required, Validators.min(1)]],
      date: ['', [Validators.required]]
    });
  }

  loadData(): void {
    this.isLoading.set(true);

    // Retrieve transactions IMMEDIATELY (without waiting for the contract)
    this.paymentTransactionService.getByContractId(this.contractId).subscribe({
      next: res => {
        if (res?.success && res.data) {
          this.transactions.set(res.data);
        } else {
          this.transactions.set([]);
        }
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur chargement transactions:', err);
        this.transactions.set([]);
        this.isLoading.set(false);
      }
    });

    // Retrieve the contract in parallel (if not provided by history)
    if (!this.contract()) {
      this.fetchContractRxJS();
    }
  }

  /**
   * Clean refactoring with RxJS to avoid callback hells (nested subscriptions)
   */
  private fetchContractRxJS(): void {
    this.financialContractService
      .getAllFinancialContractsPaged({
        keyword: this.contractId.toString(),
        page: 0,
        size: 50
      })
      .pipe(
        map(res => res?.data?.content?.find(c => c.id === this.contractId)),
        switchMap(foundContract => {
          if (foundContract) return of(foundContract);

          // Global fallback if not found by keyword
          return this.financialContractService
            .getAllFinancialContractsPaged({ page: 0, size: 100 })
            .pipe(map(res => res?.data?.content?.find(c => c.id === this.contractId)));
        }),
        catchError(err => {
          console.error('Erreur recherche contrat:', err);
          return of(null);
        })
      )
      .subscribe(finalContract => {
        if (finalContract) {
          this.contract.set(finalContract);
        }
      });
  }

  // --- MODALS ---
  openPaymentModal(): void {
    this.paymentForm.reset({
      amount: 0,
      paymentMethod: 'CASH',
      reference: ''
    });
    this.isPaymentModalOpen.set(true);
  }

  closePaymentModal(): void {
    this.isPaymentModalOpen.set(false);
  }

  closeConfirmModal(): void {
    this.isConfirmModalOpen.set(false);
  }

  openStatusModal(transaction: PaymentTransactionResponse): void {
    this.selectedTransaction.set(transaction);
    this.selectedNewStatus.set('');
    this.isStatusModalOpen.set(true);
  }

  closeStatusModal(): void {
    this.isStatusModalOpen.set(false);
    this.selectedTransaction.set(null);
    this.selectedNewStatus.set('');
  }

  openConfirmStatusModal(): void {
    if (!this.selectedTransaction() || !this.selectedNewStatus()) return;
    this.isConfirmStatusModalOpen.set(true);
  }

  closeConfirmStatusModal(): void {
    this.isConfirmStatusModalOpen.set(false);
  }

  confirmStatusUpdate(): void {
    const tx = this.selectedTransaction();
    const newStatus = this.selectedNewStatus();
    if (!tx || !tx.id || !newStatus) return;

    this.isSubmitting.set(true);
    this.paymentTransactionService.changeStatus(tx.id, newStatus).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeConfirmStatusModal();
        this.closeStatusModal();
        this.toastService.success('Le statut du règlement a été mis à jour avec succès.');
        this.loadData();
        this.fetchContractRxJS();
      },
      error: err => {
        this.isSubmitting.set(false);
        this.closeConfirmStatusModal();
        const errMsg = err.error?.message || 'Erreur de communication avec le serveur.';
        this.toastService.error(errMsg);
      }
    });
  }

  getFriendlyStatus(status?: string): string {
    const statusMap: Record<string, string> = {
      CLEARED: 'Validé',
      PENDING: 'En attente',
      BOUNCED: 'Impayé / Rejeté',
      CANCELLED: 'Annulé',
      REFUNDED: 'Remboursé'
    };
    return statusMap[status || ''] || status || '';
  }

  openInstallmentModal(): void {
    this.installmentForm.reset();
    this.isInstallmentModalOpen.set(true);
  }

  closeInstallmentModal(): void {
    this.isInstallmentModalOpen.set(false);
  }

  quickPay(installmentId: string): void {
    this.openPaymentModal();
  }

  // --- SUBMISSIONS ---
  handlePaymentSubmit(): void {
    if (this.paymentForm.invalid || this.isSubmitting()) {
      this.paymentForm.markAllAsTouched();
      return;
    }

    this.isConfirmModalOpen.set(true);
  }

  confirmPayment(): void {
    this.isSubmitting.set(true);
    const formValue = this.paymentForm.value;

    const method: PaymentTransactionRequest.PaymentMethodEnum = formValue.paymentMethod as PaymentTransactionRequest.PaymentMethodEnum;

    const payload: PaymentTransactionRequest = {
      financialContractId: this.contractId,
      amount: formValue.amount,
      paymentMethod: method,
      paymentDate: this.getLocalDateTimeString(),
      reference: formValue.reference || undefined
    };

    this.paymentTransactionService.create1(payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeConfirmModal();
        this.closePaymentModal();
        this.toastService.success('Le règlement a été enregistré avec succès.');
        this.loadData(); // Reload transactions only
      },
      error: err => {
        this.isSubmitting.set(false);
        this.closeConfirmModal();
        const errMsg = err.error?.message || 'Erreur de communication avec le serveur.';
        this.toastService.error(errMsg);
      }
    });
  }

  handleInstallmentSubmit(): void {
    if (this.installmentForm.invalid) {
      this.installmentForm.markAllAsTouched();
      return;
    }
    // TODO: implement installment creation
  }

  // --- HELPERS & ALERTS ---
  formatDate(dateStr?: string | Date): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '';

    // Use modern Intl API (cleaner than manual arrays)
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(d);
  }

  getFriendlyPaymentMethod(method?: string): string {
    const methods: Record<string, string> = {
      CASH: 'Espèces',
      CHECK: 'Chèque',
      CHEQUE: 'Chèque',
      BANK_TRANSFER: 'Virement Bancaire',
      CREDIT_CARD: 'Carte Bancaire',
      ONLINE_STRIPE: 'Stripe (En ligne)'
    };
    return methods[method || ''] || method || '';
  }

  getStatusBadgeClasses(contract: FinancialContractListResponse | null): string {
    if (!contract) return 'bg-slate-100 text-slate-600 border-slate-200';
    const statusMap: Record<string, string> = {
      FULLY_PAID: 'bg-emerald-50 text-emerald-700 border-emerald-100',
      CANCELLED: 'bg-slate-100 text-slate-800 border-slate-200',
      SETTLED_WITH_DEBT: 'bg-rose-50 text-rose-700 border-rose-100 animate-pulse',
      DRAFT: 'bg-slate-150 text-slate-600 border-slate-200'
    };
    return statusMap[contract.status as string] || 'bg-blue-50 text-blue-700 border-blue-100';
  }

  getStatusLabel(contract: FinancialContractListResponse | null): string {
    if (!contract) return 'Chargement...';
    const labelMap: Record<string, string> = {
      FULLY_PAID: 'Soldé',
      CANCELLED: 'Abandon / Annulé',
      SETTLED_WITH_DEBT: 'Dette active',
      DRAFT: 'Brouillon'
    };
    return labelMap[contract.status as string] || 'En cours / Actif';
  }

  getLocalDateTimeString(): string {
    const now = new Date();
    // Creates the local ISO format (without Z) required by many Java/Spring backends
    const offset = now.getTimezoneOffset() * 60000;
    const localISOTime = new Date(now.getTime() - offset).toISOString().slice(0, 19);
    return localISOTime;
  }

  printReceipt(receiptId: string): void {
    this.toastService.show('info', `Impression du reçu ${receiptId}`);
  }

  printReceiptPdf(transactionId: number): void {
    this.toastService.show('info', 'Préparation du reçu PDF...');
    this.paymentTransactionService
      .generateReceipt(transactionId, 'body', false, {
        httpHeaderAccept: 'application/pdf' as any
      })
      .subscribe({
        next: (response: any) => {
          // response will be a Blob since we passed httpHeaderAccept as 'application/pdf' (which falls back to blob responseType)
          const blob = new Blob([response], { type: 'application/pdf' });
          const blobUrl = URL.createObjectURL(blob);
          window.open(blobUrl, '_blank');
          setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);
        },
        error: err => {
          console.error('Erreur de téléchargement du reçu PDF:', err);
          this.toastService.error('Impossible de charger le reçu PDF.');
        }
      });
  }
}
