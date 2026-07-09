import { Routes } from '@angular/router';
import { StudentLayout } from '../../layout/student-layout/student-layout';
import { StudentDashboard } from './pages/dashboard/dashboard';
import { StudentProfileComponent } from '../../layout/student-layout/profile/profile';
import { StudentEnrollments } from './pages/enrollments/enrollments';
import { StudentEnrollmentDetail } from './pages/enrollments/detail/detail';

export const STUDENT_ROUTES: Routes = [
  {
    path: '',
    component: StudentLayout,
    children: [
      { path: 'dashboard', component: StudentDashboard },
      { path: 'profile', component: StudentProfileComponent },
      { path: 'enrollments', component: StudentEnrollments },
      { path: 'enrollments/:id', component: StudentEnrollmentDetail },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
