import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';
import { FinancialContractControllerService } from '../../../../../core/api/api/financial-contract-controller.service';
import { PaymentTransactionControllerService } from '../../../../../core/api/api/payment-transaction-controller.service';
import { FinancialContractListResponse } from '../../../../../core/api/model/financial-contract-list-response';
import { PaymentTransactionResponse } from '../../../../../core/api/model/payment-transaction-response';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-student-contract-transactions',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, NgClass],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class StudentContractTransactionsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private contractService = inject(FinancialContractControllerService);
  private transactionService = inject(PaymentTransactionControllerService);

  // State signals
  contractId = signal<number | null>(null);
  contract = signal<FinancialContractListResponse | null>(null);
  transactions = signal<PaymentTransactionResponse[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idStr = params.get('id');
      if (idStr) {
        const id = Number(idStr);
        this.contractId.set(id);
        this.loadData(id);
      }
    });
  }

  loadData(id: number): void {
    this.isLoading.set(true);
    
    // 1. Fetch contracts to find the specific contract details
    this.contractService.getMyContracts().subscribe({
      next: (response) => {
        if (response.data) {
          const found = response.data.find(c => c.id === id);
          if (found) {
            this.contract.set(found);
          }
        }
        
        // 2. Fetch transactions for this contract
        this.transactionService.getByContractId(id)
          .pipe(finalize(() => this.isLoading.set(false)))
          .subscribe({
            next: (transResponse) => {
              if (transResponse.data) {
                // Sort transactions by date descending (newest first)
                this.transactions.set(
                  transResponse.data.sort((a, b) => {
                    const dateA = a.paymentDate ? new Date(a.paymentDate).getTime() : 0;
                    const dateB = b.paymentDate ? new Date(b.paymentDate).getTime() : 0;
                    return dateB - dateA;
                  })
                );
              }
            },
            error: (err) => {
              console.error('Error fetching transactions:', err);
            }
          });
      },
      error: (err) => {
        console.error('Error fetching contract details:', err);
        this.isLoading.set(false);
      }
    });
  }

  getPaymentMethodLabel(method: string | undefined): string {
    switch (method) {
      case 'CASH':
        return 'Espèces';
      case 'CHECK':
        return 'Chèque';
      case 'BANK_TRANSFER':
        return 'Virement Bancaire';
      case 'CREDIT_CARD':
        return 'Carte Bancaire';
      case 'ONLINE_STRIPE':
        return 'Stripe en ligne';
      default:
        return method || 'Inconnu';
    }
  }

  getTransactionStatusConfig(status: string | undefined) {
    switch (status) {
      case 'PENDING':
        return { label: 'En attente', classes: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'CLEARED':
        return { label: 'Validé', classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
      case 'BOUNCED':
        return { label: 'Impayé/Rejeté', classes: 'bg-rose-50 text-rose-700 border-rose-200' };
      case 'CANCELLED':
        return { label: 'Annulé', classes: 'bg-slate-100 text-slate-500 border-slate-200' };
      case 'REFUNDED':
        return { label: 'Remboursé', classes: 'bg-purple-50 text-purple-700 border-purple-200' };
      default:
        return { label: status || 'Inconnu', classes: 'bg-slate-50 text-slate-500 border-slate-200' };
    }
  }

  getContractStatusConfig(status: string | undefined) {
    switch (status) {
      case 'DRAFT':
        return { label: 'Brouillon', classes: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'ACTIVE':
        return { label: 'Actif', classes: 'bg-blue-50 text-blue-700 border-blue-200' };
      case 'FULLY_PAID':
        return { label: 'Payé', classes: 'bg-emerald-50 text-emerald-700 border-emerald-250' };
      case 'CANCELLED':
        return { label: 'Annulé', classes: 'bg-slate-100 text-slate-500 border-slate-200' };
      case 'SETTLED_WITH_DEBT':
        return { label: 'Clôturé avec Dette', classes: 'bg-rose-50 text-rose-700 border-rose-200' };
      default:
        return { label: status || 'Inconnu', classes: 'bg-slate-50 text-slate-500 border-slate-200' };
    }
  }
}
