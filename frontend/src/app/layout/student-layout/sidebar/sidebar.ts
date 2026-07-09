import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-student-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class StudentSidebar {
  private router = inject(Router);
  public authService = inject(AuthService);

  isOpen = signal(false);

  constructor() {
    // Automatically close sidebar on navigation on mobile devices
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.isOpen.set(false);
    });
  }

  toggleSidebar() {
    this.isOpen.update(open => !open);
  }
}
