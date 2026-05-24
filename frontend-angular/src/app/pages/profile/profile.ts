import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { ThemeService, Theme } from '../../core/services/theme.service';

@Component({
  selector: 'nv-profile',
  imports: [],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile {
  private auth = inject(AuthService);
  private themeService = inject(ThemeService);
  private router = inject(Router);

  user = this.auth.currentUser;
  theme = this.themeService.theme;
  savingTheme = signal(false);

  constructor() {
    // Si entran directo a /profile (refresh) y aún no se cargó el usuario, pedirlo.
    if (!this.user() && this.auth.getToken()) {
      this.auth.loadCurrentUser().subscribe({
        error: () => this.router.navigate(['/login'])
      });
    }
  }

  setTheme(theme: Theme): void {
    this.themeService.set(theme);
    if (this.auth.isAuthenticated()) {
      this.savingTheme.set(true);
      this.auth.updateThemePreference(theme).subscribe({
        next: () => this.savingTheme.set(false),
        error: () => this.savingTheme.set(false)
      });
    }
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/dashboard']);
  }
}
