import { Routes } from '@angular/router';
import { AdminLayout } from '../../layout/admin-layout/admin-layout';
import { AcademicYear } from './pages/academic-year/academic-year'; // <-- Ton composant
import { Dashboard } from './pages/dashboard/dashboard';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: 'academic-years', component: AcademicYear },
      { path: 'dashboard', component: Dashboard },
    ],
  },
];
