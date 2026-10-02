import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { UserControllerService } from '../../../core/api/api/user-controller.service';
import { UserResponse } from '../../../core/api/model/user-response';

@Component({
  selector: 'app-account-security',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './account-security.html',
  styleUrl: './account-security.css',
  host: {
    class: 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class AccountSecurityComponent implements OnInit {
  protected authService = inject(AuthService);
  private userService = inject(UserControllerService);

  // --- State Signals ---
  user = signal<UserResponse | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);
  copiedUsername = signal<boolean>(false);

  // --- Password Change Modal / Form ---
  showPasswordModal = signal<boolean>(false);
  passwordLoading = signal<boolean>(false);
  passwordSuccess = signal<string | null>(null);
  passwordError = signal<string | null>(null);

  oldPassword = signal<string>('');
  newPassword = signal<string>('');
  confirmNewPassword = signal<string>('');

  showOld = signal<boolean>(false);
  showNew = signal<boolean>(false);
  showConfirm = signal<boolean>(false);

  ngOnInit(): void {
    this.fetchUserData();
  }

  fetchUserData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const userId = this.authService.getUserIdFromToken();
    if (!userId) {
      this.errorMessage.set("Impossible d'extraire l'identifiant utilisateur du jeton d'authentification.");
      this.isLoading.set(false);
      return;
    }

    this.userService.getUserById(userId).subscribe({
      next: (response) => {
        if (response.data) {
          this.user.set(response.data);
        } else {
          this.errorMessage.set('Aucune donnée utilisateur retournée par le serveur.');
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors de la récupération des détails utilisateur :', err);
        const backendMsg = err?.error?.message || 'Erreur lors de la récupération de vos informations.';
        this.errorMessage.set(backendMsg);
        this.isLoading.set(false);
      }
    });
  }

  copyUsername(): void {
    const username = this.user()?.username;
    if (username) {
      navigator.clipboard.writeText(username).then(() => {
        this.copiedUsername.set(true);
        setTimeout(() => this.copiedUsername.set(false), 2000);
      });
    }
  }

  openPasswordModal(): void {
    this.oldPassword.set('');
    this.newPassword.set('');
    this.confirmNewPassword.set('');
    this.passwordError.set(null);
    this.passwordSuccess.set(null);
    this.showPasswordModal.set(true);
  }

  closePasswordModal(): void {
    this.showPasswordModal.set(false);
  }

  submitPasswordChange(): void {
    const oldP = this.oldPassword().trim();
    const newP = this.newPassword().trim();
    const confirmP = this.confirmNewPassword().trim();

    this.passwordError.set(null);
    this.passwordSuccess.set(null);

    if (!oldP || !newP || !confirmP) {
      this.passwordError.set('Veuillez remplir tous les champs du mot de passe.');
      return;
    }

    if (newP !== confirmP) {
      this.passwordError.set('Le nouveau mot de passe et sa confirmation ne correspondent pas.');
      return;
    }

    if (newP.length < 6) {
      this.passwordError.set('Le nouveau mot de passe doit comporter au moins 6 caractères.');
      return;
    }

    this.passwordLoading.set(true);
    this.userService.changePassword({
      oldPassword: oldP,
      newPassword: newP,
      confirmNewPassword: confirmP
    }).subscribe({
      next: () => {
        this.passwordLoading.set(false);
        this.passwordSuccess.set('Votre mot de passe a été modifié avec succès.');
        setTimeout(() => {
          this.closePasswordModal();
        }, 1500);
      },
      error: (err) => {
        this.passwordLoading.set(false);
        this.passwordError.set(this.mapPasswordError(err));
      }
    });
  }

  private mapPasswordError(err: any): string {
    const rawError = err?.error?.errorCode || err?.error?.message || err?.error?.code || '';

    if (rawError === 'USER_OLD_PASSWORD_INCORRECT' || (typeof rawError === 'string' && rawError.includes('OLD_PASSWORD_INCORRECT'))) {
      return 'Le mot de passe actuel est incorrect.';
    }
    if (rawError === 'USER_PASSWORD_CONFIRMATION_MISMATCH' || rawError === 'PASSWORD_CONFIRMATION_MISMATCH') {
      return 'Le nouveau mot de passe et sa confirmation ne correspondent pas.';
    }
    if (rawError === 'USER_PASSWORD_TOO_WEAK' || rawError === 'PASSWORD_TOO_WEAK') {
      return 'Le nouveau mot de passe est trop faible. Veuillez choisir un mot de passe plus sécurisé.';
    }
    if (rawError === 'USER_SAME_PASSWORD' || (typeof rawError === 'string' && rawError.includes('SAME_PASSWORD'))) {
      return 'Le nouveau mot de passe doit être différent de votre mot de passe actuel.';
    }

    return err?.error?.message || 'Erreur lors du changement de mot de passe.';
  }

  // --- UI Helpers ---
  getInitials(firstName?: string, lastName?: string): string {
    const f = firstName ? firstName.charAt(0).toUpperCase() : '';
    const l = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${f}${l}` || 'U';
  }

  getRoleConfig(role?: string) {
    switch (role) {
      case 'ROLE_STUDENT':
        return { label: 'Étudiant', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
      case 'ROLE_TEACHER':
        return { label: 'Enseignant', badgeClass: 'bg-indigo-50 text-indigo-700 border-indigo-200' };
      case 'ROLE_PARENT':
        return { label: 'Parent d\'élève', badgeClass: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'ROLE_ADMIN':
        return { label: 'Administrateur', badgeClass: 'bg-purple-50 text-purple-700 border-purple-200' };
      default:
        return { label: role || 'Utilisateur', badgeClass: 'bg-slate-50 text-slate-700 border-slate-200' };
    }
  }

  getStatusConfig(status?: string) {
    switch (status) {
      case 'ACTIVE':
        return { label: 'Actif', dotClass: 'bg-emerald-500', badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
      case 'PENDING':
        return { label: 'En attente', dotClass: 'bg-amber-500', badgeClass: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'SUSPENDED':
        return { label: 'Suspendu', dotClass: 'bg-rose-500', badgeClass: 'bg-rose-50 text-rose-700 border-rose-200' };
      case 'ARCHIVED':
        return { label: 'Archivé', dotClass: 'bg-slate-500', badgeClass: 'bg-slate-50 text-slate-700 border-slate-200' };
      default:
        return { label: status || 'Inconnu', dotClass: 'bg-slate-400', badgeClass: 'bg-slate-50 text-slate-700 border-slate-200' };
    }
  }
}
