import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule, RouterModule],
  templateUrl: './sidebar.html'
})
export class Sidebar {
  private router = inject(Router);

  constructor(public authService: AuthService) {}

  isAnneeScolaireOpen: boolean = false;
  isSpecialtiesMenuOpen = false;
  isPromotionsMenuOpen: boolean = false;
}
