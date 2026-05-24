import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'nv-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);

  submit(): void {
    if (!this.email.trim() || !this.password) {
      this.error.set('Completá email y contraseña.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    this.auth.login({ email: this.email.trim(), password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/profile']);
      },
      error: (err) => {
        this.loading.set(false);
        if (err?.status === 401) {
          this.error.set('Credenciales inválidas.');
        } else if (err?.status === 0) {
          this.error.set('No se pudo conectar con el backend.');
        } else {
          this.error.set(err?.error?.message ?? 'No se pudo iniciar sesión.');
        }
      }
    });
  }
}
