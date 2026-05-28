import { Routes } from '@angular/router';
import { AdminLayout } from '../../layout/admin-layout/admin-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { AcademicYearList } from './pages/academic-year/academic-year-list/academic-year-list';
import { Level } from './pages/speciality/level/level';
import { Specialty } from './pages/speciality/specialty/specialty';
import { EnrollmentList } from './pages/enrollment/enrollment-list/enrollment-list';
import { EnrollmentCreate } from './pages/enrollment/enrollment-create/enrollment-create';
import { AcademicYearsPeriod } from './pages/academic-year/academic-years-period/academic-years-period';
import { Holiday } from './pages/academic-year/holiday/holiday';
import { Training } from './pages/speciality/training/training';
import { Accelerated } from './pages/training-session/accelerated/accelerated';
import { Continuous } from './pages/training-session/continuous/continuous';
import { Accredited } from './pages/training-session/accredited/accredited';
import { EnrollmentDocumentComponent } from './pages/enrollment-document/enrollment-document';
import { EnrollmentDetail } from './pages/enrollment/enrollment-detail/enrollment-detail';
import { StudentList } from './pages/profile/students/student-list/student-list';
import { StudentDetail } from './pages/profile/students/student-detail/student-detail';
import { AccountList } from './pages/account/account-list/account-list';
import { TeacherSpecialties } from './pages/profile/teacher/teacher-specialties/teacher-specialties';
import { TeacherList } from './pages/profile/teacher/teacher-list/teacher-list';
import { SubjectList } from './pages/subjects/subject-list/subject-list';
import { PromotionSubject } from './pages/training-session/promotion-subject/promotion-subject';
import { ClassmanagementList } from './pages/classmanagement/classmanagement-list/classmanagement-list';
import { ClassStudentList } from './pages/classmanagement/class-student-list/class-student-list';
import { Room } from './pages/room/room';

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
      { path: 'enrollment-documents', component: EnrollmentDocumentComponent },
      { path: 'enrollments', component: EnrollmentList },
      { path: 'enrollments/new', component: EnrollmentCreate },
      { path: 'enrollments/details/:id', component: EnrollmentDetail },
      { path: 'holidays', component: Holiday },
      { path: 'training', component: Training },
      { path: 'promotions/accelerated', component: Accelerated },
      { path: 'promotions/continuous', component: Continuous },
      { path: 'promotions/accredited', component: Accredited },
      { path: 'students', component: StudentList },
      { path: 'students/details/:id', component: StudentDetail },
      { path: 'account', component: AccountList },
      { path: 'teacher-specialties', component: TeacherSpecialties },
      { path: 'teachers', component: TeacherList },
      { path: 'modules', component: SubjectList },
      { path: 'promotions/accredited/:id/subjects', component: PromotionSubject },
      { path: 'promotions/continuous/:id/subjects', component: PromotionSubject },
      { path: 'promotions/accelerated/:id/subjects', component: PromotionSubject },
      { path: 'class-management', component: ClassmanagementList },
      { path: 'class-management/:id/students', component: ClassStudentList },
      { path: 'rooms', component: Room }
    ]
  }
];
