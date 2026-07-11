import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FinancialContractControllerService } from '../../../../core/api/api/financial-contract-controller.service';
import { FinancialContractListResponse } from '../../../../core/api/model/financial-contract-list-response';

@Component({
  selector: 'app-student-contracts',
  standalone: true,
  imports: [CurrencyPipe, NgClass, RouterLink],
  templateUrl: './contracts.html',
  styleUrl: './contracts.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class StudentContractsComponent implements OnInit {
  private contractService = inject(FinancialContractControllerService);

  // State signals
  contracts = signal<FinancialContractListResponse[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.fetchContracts();
  }

  fetchContracts(): void {
    this.isLoading.set(true);
    this.contractService.getMyContracts().subscribe({
      next: (response) => {
        if (response.data) {
          this.contracts.set(response.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching financial contracts:', err);
        this.isLoading.set(false);
      }
    });
  }

  getStatusConfig(status: string | undefined) {
    switch (status) {
      case 'DRAFT':
        return { label: 'Brouillon', classes: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'ACTIVE':
        return { label: 'Actif', classes: 'bg-blue-50 text-blue-700 border-blue-200' };
      case 'FULLY_PAID':
        return { label: 'Payé', classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
      case 'CANCELLED':
        return { label: 'Annulé', classes: 'bg-slate-100 text-slate-500 border-slate-200' };
      case 'SETTLED_WITH_DEBT':
        return { label: 'Clôturé avec Dette', classes: 'bg-rose-50 text-rose-700 border-rose-200' };
      default:
        return { label: status || 'Inconnu', classes: 'bg-slate-50 text-slate-500 border-slate-200' };
    }
  }
}
