import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'nv-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {
  private auth = inject(AuthService);
  private router = inject(Router);

  username = '';
  displayName = '';
  email = '';
  password = '';
  confirmPassword = '';
  loading = signal(false);
  error = signal<string | null>(null);

  submit(): void {
    if (!this.username.trim() || !this.email.trim() || !this.password) {
      this.error.set('Completá usuario, email y contraseña.');
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.error.set('Las contraseñas no coinciden.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    this.auth.register({
      username: this.username.trim(),
      email: this.email.trim(),
      displayName: this.displayName.trim() || this.username.trim(),
      password: this.password
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        if (err?.status === 409) {
          this.error.set(err?.error?.message ?? 'El usuario o email ya está registrado.');
        } else if (err?.status === 0) {
          this.error.set('No se pudo conectar con el backend.');
        } else {
          this.error.set(err?.error?.message ?? 'No se pudo crear la cuenta.');
        }
      }
    });
  }
}
