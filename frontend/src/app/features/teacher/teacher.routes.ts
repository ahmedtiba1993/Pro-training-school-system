import { Routes } from '@angular/router';
import { TeacherLayout } from '../../layout/teacher-layout/teacher-layout';
import { TeacherDashboard } from './pages/dashboard/dashboard';

export const TEACHER_ROUTES: Routes = [
  {
    path: '',
    component: TeacherLayout,
    children: [
      { path: 'dashboard', component: TeacherDashboard },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
