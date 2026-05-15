import {Component, inject, signal} from '@angular/core';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from '../../../core/auth/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html'
})
export class Sidebar {
  // Rétabli comme dans ton code d'origine
  private router = inject(Router);
  public authService = inject(AuthService);

  // Using Signals for menu state
  isAnneeScolaireOpen = signal<boolean>(false);
  isPromotionsMenuOpen = signal<boolean>(false);
  isSpecialtiesMenuOpen = signal<boolean>(false);

  togglePromotions(): void {
    this.isPromotionsMenuOpen.update(state => !state);
  }

  toggleSpecialties(): void {
    this.isSpecialtiesMenuOpen.update(state => !state);
  }

  toggleAnneeScolaire(): void {
    this.isAnneeScolaireOpen.update(state => !state);
  }
}
