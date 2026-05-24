import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  // Bootstrap temprano: aplica el tema persistido y restaura la sesión si hay token.
  private readonly theme = inject(ThemeService);
  private readonly auth = inject(AuthService);
}
