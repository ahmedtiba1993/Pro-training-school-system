import { Component, OnInit, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { PaymentTransactionControllerService, PaymentTransactionResponse } from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-payment-transaction-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './payment-transaction-list.html',
  styleUrl: './payment-transaction-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentTransactionList implements OnInit {
  private readonly paymentTransactionService = inject(PaymentTransactionControllerService);
  private readonly toastService = inject(ToastService);

  // --- STATE (Signals) ---
  transactions = signal<PaymentTransactionResponse[]>([]);
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);

  // --- STATE (Pagination) ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // --- STATE (Filters) ---
  startDate = signal<string>('');
  endDate = signal<string>('');

  // --- STATE (Status Modal) ---
  isStatusModalOpen = signal<boolean>(false);
  isConfirmStatusModalOpen = signal<boolean>(false);
  selectedTransaction = signal<PaymentTransactionResponse | null>(null);
  selectedNewStatus = signal<PaymentTransactionResponse.StatusEnum | ''>('');

  // --- COMPUTED STATES (Pagination bounds indicators) ---
  startElement = computed(() => {
    if (this.transactions().length === 0) return 0;
    return this.currentPage() * this.pageSize() + 1;
  });

  endElement = computed(() => {
    const count = this.transactions().length;
    return this.currentPage() * this.pageSize() + count;
  });

  ngOnInit(): void {
    this.loadTransactions();
  }

  // --- API CALLS ---
  loadTransactions(): void {
    this.isLoading.set(true);

    const start = this.startDate() ? this.startDate() : undefined;
    const end = this.endDate() ? this.endDate() : undefined;

    this.paymentTransactionService.getAllPaged1(
      start,
      end,
      this.currentPage(),
      this.pageSize()
    ).subscribe({
      next: (response) => {
        if (response && response.success && response.data) {
          const pageData = response.data;
          this.transactions.set(pageData.content || []);
          this.totalElements.set(pageData.totalElements || 0);
          this.totalPages.set(pageData.totalPages || 0);
        } else {
          this.transactions.set([]);
          this.totalElements.set(0);
          this.totalPages.set(0);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading payment transactions:', err);
        this.toastService.error('Erreur lors du chargement des transactions de paiement.');
        this.transactions.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.isLoading.set(false);
      }
    });
  }

  // --- HANDLERS & ACTIONS ---
  applyFilters(): void {
    this.currentPage.set(0);
    this.loadTransactions();
  }

  resetFilters(): void {
    this.startDate.set('');
    this.endDate.set('');
    this.currentPage.set(0);
    this.loadTransactions();
  }

  onPageSizeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.pageSize.set(Number(select.value));
    this.currentPage.set(0);
    this.loadTransactions();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadTransactions();
    }
  }

  // --- STATUS EDIT MODAL METHODS ---
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
        this.loadTransactions();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.closeConfirmStatusModal();
        const errMsg = err.error?.message || 'Erreur de communication avec le serveur.';
        this.toastService.error(errMsg);
      }
    });
  }

  // --- PRINT RECEIPT ---
  printReceiptPdf(transactionId: number): void {
    this.toastService.show('info', 'Préparation du reçu PDF...');
    this.paymentTransactionService.generateReceipt(transactionId, 'body', false, {
      httpHeaderAccept: 'application/pdf' as any
    }).subscribe({
      next: (response: any) => {
        const blob = new Blob([response], { type: 'application/pdf' });
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl, '_blank');
        setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);
      },
      error: (err) => {
        console.error('Erreur de téléchargement du reçu PDF:', err);
        this.toastService.error('Impossible de charger le reçu PDF.');
      }
    });
  }

  // --- HELPERS ---
  formatDate(dateStr?: string | Date): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '';
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    }).format(d);
  }

  getFriendlyPaymentMethod(method?: string): string {
    const methods: Record<string, string> = {
      'CASH': 'Espèces',
      'CHECK': 'Chèque',
      'CHEQUE': 'Chèque',
      'BANK_TRANSFER': 'Virement Bancaire',
      'CREDIT_CARD': 'Carte Bancaire',
      'ONLINE_STRIPE': 'Stripe (En ligne)'
    };
    return methods[method || ''] || method || '';
  }

  getFriendlyStatus(status?: string): string {
    const statusMap: Record<string, string> = {
      'CLEARED': 'Validé',
      'PENDING': 'En attente',
      'BOUNCED': 'Impayé / Rejeté',
      'CANCELLED': 'Annulé',
      'REFUNDED': 'Remboursé'
    };
    return statusMap[status || ''] || status || '';
  }
}
