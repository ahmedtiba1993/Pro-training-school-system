import { Routes } from '@angular/router';
import { ParentLayout } from '../../layout/parent-layout/parent-layout';
import { ParentDashboard } from './pages/dashboard/dashboard';
import { ParentSecurity } from './pages/security/security';
import { ParentCalendar } from './pages/calendar/calendar';

export const PARENT_ROUTES: Routes = [
  {
    path: '',
    component: ParentLayout,
    children: [
      { path: 'dashboard', component: ParentDashboard },
      { path: 'calendar', component: ParentCalendar },
      { path: 'security', component: ParentSecurity },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
