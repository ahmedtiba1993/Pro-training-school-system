import { Routes } from '@angular/router';
import { ParentLayout } from '../../layout/parent-layout/parent-layout';
import { ParentDashboard } from './pages/dashboard/dashboard';

export const PARENT_ROUTES: Routes = [
  {
    path: '',
    component: ParentLayout,
    children: [
      { path: 'dashboard', component: ParentDashboard },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
