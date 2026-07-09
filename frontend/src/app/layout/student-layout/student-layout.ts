import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { StudentSidebar } from './sidebar/sidebar';

@Component({
  selector: 'app-student-layout',
  standalone: true,
  imports: [RouterOutlet, StudentSidebar],
  templateUrl: './student-layout.html',
})
export class StudentLayout {}
