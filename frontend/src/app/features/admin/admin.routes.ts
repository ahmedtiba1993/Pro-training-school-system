import { Routes } from '@angular/router';
import { AdminLayout } from '../../layout/admin-layout/admin-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { AcademicYearList } from './pages/academic-year/academic-year-list/academic-year-list';
import { AcademicYearTerms } from './pages/academic-year/academic-year-terms/academic-year-terms';
import { TermSessions } from './pages/academic-year/term-sessions/term-sessions';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: 'academic-years', component: AcademicYearList },
      { path: 'academic-years/:id/terms', component: AcademicYearTerms },
      { path: 'terms/:termId/sessions', component: TermSessions },

      { path: 'dashboard', component: Dashboard },
    ],
  },
];
