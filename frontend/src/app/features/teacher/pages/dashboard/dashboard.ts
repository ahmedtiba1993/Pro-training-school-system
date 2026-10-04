import { Component, inject } from '@angular/core';
import { AuthService } from '../../../../core/auth/auth.service';

import { AcademicYearCardComponent } from '../../../../shared/components/academic-year-card/academic-year-card';

@Component({
  selector: 'app-teacher-dashboard',
  imports: [AcademicYearCardComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class TeacherDashboard {
  public authService = inject(AuthService);
}
