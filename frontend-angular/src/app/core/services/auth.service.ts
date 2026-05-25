import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from '../api.config';
import { AuthResponse, CurrentUser, LoginRequest, RegisterRequest, ThemePreference } from '../models/auth.model';
import { ThemeService } from './theme.service';

const TOKEN_KEY = 'nv_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private themeService = inject(ThemeService);

  private currentUserSig = signal<CurrentUser | null>(null);
  readonly currentUser = this.currentUserSig.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSig() !== null);

  constructor() {
    // Restaurar sesión si hay token guardado.
    if (this.getToken()) {
      this.loadCurrentUser().subscribe({ error: () => this.clearSession() });
    }
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/register`, request)
      .pipe(tap(res => this.handleAuth(res)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/login`, request)
      .pipe(tap(res => this.handleAuth(res)));
  }

  loadCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${API_BASE_URL}/auth/me`)
      .pipe(tap(user => this.applyUser(user)));
  }

  updateThemePreference(theme: ThemePreference): Observable<CurrentUser> {
    return this.http.put<CurrentUser>(`${API_BASE_URL}/auth/me/preferences`, { themePreference: theme })
      .pipe(tap(user => this.currentUserSig.set(user)));
  }

  logout(): void {
    if (this.getToken()) {
      this.http.post(`${API_BASE_URL}/auth/logout`, {}).subscribe({ next: () => {}, error: () => {} });
    }
    this.clearSession();
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private handleAuth(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    this.applyUser(res.user);
  }

  private applyUser(user: CurrentUser): void {
    this.currentUserSig.set(user);
    if (user.themePreference === 'dark' || user.themePreference === 'light') {
      this.themeService.set(user.themePreference);
    }
  }

  /** Limpia el estado local (token + usuario). No hace request al backend. */
  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUserSig.set(null);
  }
}
