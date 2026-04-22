import { Routes } from '@angular/router';
import { AdminLayout } from '../../layout/admin-layout/admin-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { AcademicYearList } from './pages/academic-year/academic-year-list/academic-year-list';
import { Level } from './pages/speciality/level/level';
import { Specialty } from './pages/speciality/specialty/specialty';
import { RegistrationDocument } from '../../../app/features/admin/pages/registration-document/registration-document';
import { TrainingSession } from './pages/training-session/training-session';
import { EnrollmentList } from './pages/enrollment/enrollment-list/enrollment-list';
import { EnrollmentCreate } from './pages/enrollment/enrollment-create/enrollment-create';
import { AcademicYearsPeriod } from './pages/academic-year/academic-years-period/academic-years-period';
import { Holiday } from './pages/academic-year/holiday/holiday';
import { Training } from './pages/speciality/training/training';
import { Accelerated } from './pages/training-session/accelerated/accelerated';
import { Continuous } from './pages/training-session/continuous/continuous';
import { Accredited } from './pages/training-session/accredited/accredited';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayout,
    children: [
      { path: 'academic-years', component: AcademicYearList },
      { path: 'academic-years/:id/periods', component: AcademicYearsPeriod },
      { path: 'levels', component: Level },
      { path: 'specialties', component: Specialty },
      { path: 'dashboard', component: Dashboard },
      { path: 'registration-documents', component: RegistrationDocument },
      { path: 'promotions', component: TrainingSession },
      { path: 'enrollments', component: EnrollmentList },
      { path: 'enrollments/new', component: EnrollmentCreate },
      { path: 'holidays', component: Holiday },
      { path: 'training', component: Training },
      { path: 'promotions/accelerated', component: Accelerated },
      { path: 'promotions/continuous', component: Continuous },
      { path: 'promotions/accredited', component: Accredited }
    ]
  }
];
