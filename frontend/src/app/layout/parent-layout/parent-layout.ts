import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ParentSidebar } from './sidebar/sidebar';

@Component({
  selector: 'app-parent-layout',
  standalone: true,
  imports: [RouterOutlet, ParentSidebar],
  templateUrl: './parent-layout.html',
})
export class ParentLayout {}
