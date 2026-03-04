import { Routes } from '@angular/router';
import { roleGuard } from './core/guards/role.guard';
import { ErrorPage } from './shared/pages/error-page/error-page';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full',
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'error',
    component: ErrorPage,
  },
  {
    path: '**',
    redirectTo: 'error',
  },
];
