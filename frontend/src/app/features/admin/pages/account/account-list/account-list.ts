import {Component, inject, OnInit, signal} from '@angular/core';
import {NgClass} from '@angular/common';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';

import {UserControllerService} from '../../../../../core/api/api/user-controller.service';
import {UserResponse} from '../../../../../core/api/model/user-response';
import {AdminChangePasswordRequest} from '../../../../../core/api/model/admin-change-password-request';

import {PaginationComponent} from '../../../../../shared/components/pagination/pagination';

type ActionType = 'SUSPEND' | 'REACTIVATE' | 'ARCHIVE';

@Component({
  selector: 'app-account-list',
  standalone: true,
  imports: [NgClass, PaginationComponent, ReactiveFormsModule],
  templateUrl: './account-list.html'
})
export class AccountList implements OnInit {
  private readonly userApiService = inject(UserControllerService);

  // --- STATE (Signals) ---
  users = signal<UserResponse[]>([]);
  isLoading = signal<boolean>(false);
  error = signal<string | null>(null);
  processingUserId = signal<number | null>(null);

  // --- FILTERS (Signals) ---
  searchKeyword = signal<string>('');
  searchRole = signal<string>('');
  searchStatus = signal<string>('');

  // --- PAGINATION (Signals) ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(5);
  totalElements = signal<number>(0);

  // --- MODALS (Signals) ---
  isPasswordModalOpen = signal<boolean>(false);
  isChangingPassword = signal<boolean>(false);
  selectedUserIdForPassword = signal<number | null>(null);
  passwordError = signal<string | null>(null);

  confirmModal = signal<{ isOpen: boolean; type: ActionType | null; userId: number | null }>({
    isOpen: false,
    type: null,
    userId: null
  });

  // --- FORM ---
  passwordForm = new FormGroup(
    {
      newPassword: new FormControl('', [Validators.required, Validators.minLength(6)]),
      confirmNewPasswd: new FormControl('', [Validators.required])
    },
    {validators: this.passwordMatchValidator}
  );

  ngOnInit(): void {
    this.loadUsers();
  }

  // --- FILTER METHODS ---

  updateKeyword(event: Event): void {
    this.searchKeyword.set((event.target as HTMLInputElement).value);
  }

  updateRole(event: Event): void {
    this.searchRole.set((event.target as HTMLSelectElement).value);
  }

  updateStatus(event: Event): void {
    this.searchStatus.set((event.target as HTMLSelectElement).value);
  }

  onSearch(): void {
    this.currentPage.set(0); // Reset page to 0 on a new search
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.error.set(null);

    // Get values
    const kw = this.searchKeyword() || undefined;

    const role = (this.searchRole() || undefined) as
      | 'ROLE_ADMIN'
      | 'ROLE_TEACHER'
      | 'ROLE_STUDENT'
      | 'ROLE_PARENT'
      | undefined;
    const status = (this.searchStatus() || undefined) as
      | 'PENDING'
      | 'ACTIVE'
      | 'SUSPENDED'
      | 'ARCHIVED'
      | undefined;

    // API call with the correct types
    this.userApiService
      .getAllUsersPaged(kw, role, status, this.currentPage(), this.pageSize())
      .subscribe({
        next: response => {
          const content = response.data?.content || [];
          this.totalElements.set(response?.data?.totalElements || 0);
          this.users.set(content);
          this.isLoading.set(false);
        },
        error: err => {
          this.error.set(err.error?.message || 'Error loading users');
          this.isLoading.set(false);
        }
      });
  }

  onPageChange(newPage: number): void {
    this.currentPage.set(newPage);
    this.loadUsers();
  }

  // --- STATUS ACTIONS ---
  openConfirmModal(type: ActionType, userId: number): void {
    this.confirmModal.set({isOpen: true, type, userId});
  }

  closeConfirmModal(): void {
    this.confirmModal.set({isOpen: false, type: null, userId: null});
  }

  executeConfirmAction(): void {
    const {type, userId} = this.confirmModal();
    if (!userId || !type) return;
    this.closeConfirmModal();
    if (type === 'SUSPEND') this.suspendAccount(userId);
    if (type === 'REACTIVATE') this.reactivateAccount(userId);
    if (type === 'ARCHIVE') this.archiveAccount(userId);
  }

  private suspendAccount(id: number): void {
    this.processingUserId.set(id);
    this.error.set(null);
    this.userApiService.suspendUser(id).subscribe({
      next: () => {
        this.processingUserId.set(null);
        this.loadUsers();
      },
      error: err => {
        this.error.set(err.error?.message || 'Error during suspension');
        this.processingUserId.set(null);
      }
    });
  }

  private reactivateAccount(id: number): void {
    this.processingUserId.set(id);
    this.error.set(null);
    this.userApiService.reactivateUser(id).subscribe({
      next: () => {
        this.processingUserId.set(null);
        this.loadUsers();
      },
      error: err => {
        this.error.set(err.error?.message || 'Error during reactivation');
        this.processingUserId.set(null);
      }
    });
  }

  private archiveAccount(id: number): void {
    this.processingUserId.set(id);
    this.error.set(null);
    this.userApiService.archiveUser(id).subscribe({
      next: () => {
        this.processingUserId.set(null);
        this.loadUsers();
      },
      error: err => {
        this.error.set(err.error?.message || "Error during archiving");
        this.processingUserId.set(null);
      }
    });
  }

  // --- PASSWORD ACTIONS  ---
  openPasswordModal(userId: number): void {
    this.selectedUserIdForPassword.set(userId);
    this.passwordForm.reset();
    this.passwordError.set(null);
    this.isPasswordModalOpen.set(true);
  }

  closePasswordModal(): void {
    this.isPasswordModalOpen.set(false);
    this.selectedUserIdForPassword.set(null);
    this.passwordForm.reset();
  }

  submitPasswordChange(): void {
    if (this.passwordForm.invalid || !this.selectedUserIdForPassword()) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.isChangingPassword.set(true);
    this.passwordError.set(null);

    const request: AdminChangePasswordRequest = {
      newPassword: this.passwordForm.value.newPassword!,
      confirmNewPassword: this.passwordForm.value.confirmNewPasswd!
    };
    const userId = this.selectedUserIdForPassword()!;

    this.userApiService.changePassword(userId, request).subscribe({
      next: () => {
        this.isChangingPassword.set(false);
        this.closePasswordModal();
        this.loadUsers();
      },
      error: err => {
        this.passwordError.set(err.error?.message || 'An error occurred.');
        this.isChangingPassword.set(false);
      }
    });
  }

  private passwordMatchValidator(form: AbstractControl): ValidationErrors | null {
    const password = form.get('newPassword')?.value;
    const confirm = form.get('confirmNewPasswd')?.value;
    if (password && confirm && password !== confirm) {
      form.get('confirmNewPasswd')?.setErrors({passwordMismatch: true});
      return {passwordMismatch: true};
    }
    if (form.get('confirmNewPasswd')?.hasError('passwordMismatch'))
      form.get('confirmNewPasswd')?.setErrors(null);
    return null;
  }

  // --- UI HELPERS  ---
  getInitials(user: UserResponse): string {
    if (user.firstName && user.lastName)
      return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
    if (user.username) return user.username.substring(0, 2).toUpperCase();
    return '??';
  }

  getFullName(user: UserResponse): string {
    if (user.firstName && user.lastName)
      return `${user.firstName.charAt(0).toUpperCase() +
      user.firstName.slice(1).toLowerCase()} ${user.lastName.toUpperCase()}`;
    return user.username || 'Inconnu';
  }

  getRoleFormat(role?: string): { label: string; class: string } {
    switch (role) {
      case 'ROLE_ADMIN':
        return {
          label: 'Administrateur',
          class: 'bg-purple-100 text-purple-700 border-purple-200'
        };
      case 'ROLE_STUDENT':
        return {label: 'Apprenant', class: 'bg-indigo-50 text-indigo-700 border-indigo-100'};
      case 'ROLE_PARENT':
        return {label: 'Parent', class: 'bg-teal-50 text-teal-700 border-teal-100'};
      default:
        return {
          label: role ? role.replace('ROLE_', '') : 'Inconnu',
          class: 'bg-slate-100 text-slate-700 border-slate-200'
        };
    }
  }

  getStatusFormat(status?: string): { label: string; class: string } {
    switch (status) {
      case 'ACTIVE':
        return {label: 'Actif', class: 'bg-emerald-50 text-emerald-700 border-emerald-100'};
      case 'PENDING':
        return {label: 'En attente', class: 'bg-amber-50 text-amber-700 border-amber-100'};
      case 'SUSPENDED':
        return {label: 'Suspendu', class: 'bg-red-50 text-red-700 border-red-100'};
      case 'ARCHIVED':
        return {label: 'Archivé', class: 'bg-slate-100 text-slate-600 border-slate-200'};
      default:
        return {label: status || 'Inconnu', class: 'bg-gray-100 text-gray-700 border-gray-200'};
    }
  }
}
