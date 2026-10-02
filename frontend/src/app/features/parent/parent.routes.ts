import { Routes } from '@angular/router';
import { ParentLayout } from '../../layout/parent-layout/parent-layout';
import { ParentDashboard } from './pages/dashboard/dashboard';
import { ParentSecurity } from './pages/security/security';

export const PARENT_ROUTES: Routes = [
  {
    path: '',
    component: ParentLayout,
    children: [
      { path: 'dashboard', component: ParentDashboard },
      { path: 'security', component: ParentSecurity },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
