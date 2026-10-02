import { Routes } from '@angular/router';
import { TeacherLayout } from '../../layout/teacher-layout/teacher-layout';
import { TeacherDashboard } from './pages/dashboard/dashboard';
import { TeacherProfileComponent } from '../../layout/teacher-layout/profile/profile';
import { TeacherSchedules } from './pages/schedules/schedules';
import { TeacherSecurity } from './pages/security/security';

export const TEACHER_ROUTES: Routes = [
  {
    path: '',
    component: TeacherLayout,
    children: [
      { path: 'dashboard', component: TeacherDashboard },
      { path: 'profile', component: TeacherProfileComponent },
      { path: 'security', component: TeacherSecurity },
      { path: 'schedules', component: TeacherSchedules },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
