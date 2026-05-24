import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../core/services/auth.service';
import { ThemeService } from '../core/services/theme.service';

@Component({
  selector: 'nv-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.scss'
})
export class Shell {
  private themeService = inject(ThemeService);
  protected auth = inject(AuthService);

  theme = this.themeService.theme;

  toggleTheme(): void {
    this.themeService.toggle();
    // Si hay sesión, persistir la preferencia en el backend.
    if (this.auth.isAuthenticated()) {
      this.auth.updateThemePreference(this.theme()).subscribe({ next: () => {}, error: () => {} });
    }
  }
}
