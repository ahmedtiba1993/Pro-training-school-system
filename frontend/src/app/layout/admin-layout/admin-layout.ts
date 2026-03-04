import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
// Importe tes nouveaux composants ici :
import { Sidebar } from './sidebar/sidebar';
import { Header } from './header/header';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, Sidebar, Header],
  templateUrl: './admin-layout.html',
})
export class AdminLayout {}
