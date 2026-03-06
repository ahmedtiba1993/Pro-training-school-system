import { Routes } from '@angular/router';
import { AdminLayout } from '../../layout/admin-layout/admin-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { AcademicYearList } from './pages/academic-year/academic-year-list/academic-year-list';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: 'academic-years', component: AcademicYearList },
      { path: 'dashboard', component: Dashboard },
    ],
  },
];
