import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthRequest } from '../../../core/api/model/models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);

  // Signals used to control UI state without reloading the page
  isLoading = signal(false);
  showPassword = signal(false);

  errorMessage = signal<string>('');

  // Form definition
  loginForm = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(5)]]
  });

  ngOnInit(): void {
    if (this.authService.hasValidToken()) {
      const role = localStorage.getItem('role');
      this.redirectByRole(role);
    }
  }

  private redirectByRole(role: string | null): void {
    const forceChange = localStorage.getItem('forcePasswordChange') === 'true';
    if (forceChange) {
      this.router.navigate(['/auth/change-password']);
      return;
    }

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

  // Toggle password visibility
  togglePasswordVisibility() {
    this.showPassword.update(val => !val);
  }

  // Action triggered when the user clicks the submit button
  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading.set(true);

      const request: AuthRequest = {
        username: this.loginForm.value.username!,
        password: this.loginForm.value.password!
      };

      this.authService.login(request).subscribe({
        next: response => {
          console.log(response);
          const role = response.data?.user?.role || null;
          this.redirectByRole(role);
        },
        error: err => {
          this.isLoading.set(false);
          if (err.error && err.error.errorCode === 'BAD_CREDENTIALS') {
            this.errorMessage.set('Identifiant ou mot de passe incorrect.');
          } else if (err.error && err.error.errorCode === 'ACCOUNT_SUSPENDED') {
            this.errorMessage.set('Votre compte est temporairement suspendu. Veuillez contacter l\'administration.');
          } else if (err.status === 0) {
            this.errorMessage.set('Impossible de contacter le serveur. Vérifiez votre connexion.');
          } else {
            this.errorMessage.set(err.error?.message || 'Une erreur inattendue est survenue.');
          }
        }
      });
    }
  }
}
