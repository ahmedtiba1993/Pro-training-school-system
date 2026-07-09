import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.html',
  styleUrl: './change-password.css'
})
export class ChangePassword implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  public authService = inject(AuthService);
  private toastService = inject(ToastService);

  isLoading = signal(false);
  showPassword = signal(false);
  errorMessage = signal<string>('');

  // Password change form
  changeForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmNewPassword: ['', [Validators.required]]
  }, {
    validators: this.passwordMatchValidator
  });

  ngOnInit(): void {
    const token = localStorage.getItem('token');
    const forceChange = localStorage.getItem('forcePasswordChange') === 'true';
    if (!token) {
      this.router.navigate(['/auth/login']);
    } else if (!forceChange) {
      const role = localStorage.getItem('role');
      this.redirectByRole(role);
    }
  }

  passwordMatchValidator(control: AbstractControl) {
    const newPassword = control.get('newPassword')?.value;
    const confirmNewPassword = control.get('confirmNewPassword')?.value;
    return newPassword === confirmNewPassword ? null : { mismatch: true };
  }

  togglePasswordVisibility() {
    this.showPassword.update(val => !val);
  }

  onSubmit() {
    if (this.changeForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set('');

      const newPassword = this.changeForm.value.newPassword!;
      const confirmNewPassword = this.changeForm.value.confirmNewPassword!;

      this.authService.changePasswordFirstLogin(newPassword, confirmNewPassword).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.toastService.success('Votre mot de passe a été modifié avec succès. Veuillez vous reconnecter.');
          this.authService.logout();
        },
        error: err => {
          this.isLoading.set(false);
          if (err.error && err.error.message) {
            this.errorMessage.set(err.error.message);
          } else {
            this.errorMessage.set('Une erreur est survenue lors de la modification de votre mot de passe.');
          }
        }
      });
    }
  }

  private redirectByRole(role: string | null): void {
    switch (role) {
      case 'ROLE_ADMIN':
        this.router.navigate(['/admin/dashboard']);
        break;
      case 'ROLE_STUDENT':
        this.router.navigate(['/student/dashboard']);
        break;
      case 'ROLE_PARENT':
        this.router.navigate(['/parent/dashboard']);
        break;
      case 'ROLE_TEACHER':
        this.router.navigate(['/teacher/dashboard']);
        break;
      default:
        this.router.navigate(['/']);
        break;
    }
  }
}
