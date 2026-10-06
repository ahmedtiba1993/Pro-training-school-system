import { Component } from '@angular/core';
import { AcademicCalendarComponent } from '../../../../shared/components/academic-calendar/academic-calendar';

@Component({
  selector: 'app-student-calendar',
  standalone: true,
  imports: [AcademicCalendarComponent],
  template: `<app-academic-calendar class="flex-1 flex flex-col min-h-0 overflow-hidden" />`,
  host: {
    class: 'flex-1 flex flex-col min-h-0 overflow-hidden',
  },
})
export class StudentCalendar {}
