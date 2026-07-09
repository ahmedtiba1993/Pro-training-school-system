import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TeacherSidebar } from './sidebar/sidebar';

@Component({
  selector: 'app-teacher-layout',
  standalone: true,
  imports: [RouterOutlet, TeacherSidebar],
  templateUrl: './teacher-layout.html',
})
export class TeacherLayout {}
