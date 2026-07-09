import { Routes } from '@angular/router';
import { roleGuard } from './core/guards/role.guard';
import { ErrorPage } from './shared/pages/error-page/error-page';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full',
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'student',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_STUDENT'] },
    loadChildren: () => import('./features/student/student.routes').then((m) => m.STUDENT_ROUTES),
  },
  {
    path: 'parent',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_PARENT'] },
    loadChildren: () => import('./features/parent/parent.routes').then((m) => m.PARENT_ROUTES),
  },
  {
    path: 'teacher',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_TEACHER'] },
    loadChildren: () => import('./features/teacher/teacher.routes').then((m) => m.TEACHER_ROUTES),
  },


  {
    path: 'error',
    component: ErrorPage,
  },
  {
    path: '**',
    redirectTo: 'error',
  },
];
