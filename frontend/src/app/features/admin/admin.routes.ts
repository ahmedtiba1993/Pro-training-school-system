import { Routes } from '@angular/router';
import { AdminLayout } from '../../layout/admin-layout/admin-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { AcademicYearList } from './pages/academic-year/academic-year-list/academic-year-list';
import { AcademicYearTerms } from './pages/academic-year/academic-year-terms/academic-year-terms';
import { Level } from './pages/speciality/level/level';
import { Specialty } from './pages/speciality/specialty/specialty';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: 'academic-years', component: AcademicYearList },
      { path: 'academic-years/:id/terms', component: AcademicYearTerms },
      { path: 'levels', component: Level },
      { path: 'specialties', component: Specialty },
      { path: 'dashboard', component: Dashboard }
    ]
  }
];
