import { Routes } from '@angular/router';
import { TeacherLayout } from '../../layout/teacher-layout/teacher-layout';
import { TeacherDashboard } from './pages/dashboard/dashboard';
import { TeacherProfileComponent } from '../../layout/teacher-layout/profile/profile';
import { TeacherSchedules } from './pages/schedules/schedules';

export const TEACHER_ROUTES: Routes = [
  {
    path: '',
    component: TeacherLayout,
    children: [
      { path: 'dashboard', component: TeacherDashboard },
      { path: 'profile', component: TeacherProfileComponent },
      { path: 'schedules', component: TeacherSchedules },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
