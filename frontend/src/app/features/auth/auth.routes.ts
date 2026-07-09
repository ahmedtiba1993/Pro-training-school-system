import { Routes } from '@angular/router';
import { Login } from './login/login';
import { ChangePassword } from './change-password/change-password';

export const AUTH_ROUTES: Routes = [
  { path: 'login', component: Login },
  { path: 'change-password', component: ChangePassword },
  
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];