import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
// Importe tes nouveaux composants ici :
import { Sidebar } from './sidebar/sidebar';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, Sidebar],
  templateUrl: './admin-layout.html',
})
export class AdminLayout {}
