import { Routes } from '@angular/router';
import { AdminLayout } from '../../layout/admin-layout/admin-layout';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [{ path: '', redirectTo: 'dashboard', pathMatch: 'full' }],
  },
  {
    path: 'dashboard',
    component: AdminLayout,
    children: [{ path: '', redirectTo: 'dashboard', pathMatch: 'full' }],
  },
];
