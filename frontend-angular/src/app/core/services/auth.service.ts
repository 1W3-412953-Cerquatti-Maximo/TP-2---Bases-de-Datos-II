import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';

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

  // true mientras se valida la sesión al cargar la app (hay token pero /auth/me aún no respondió).
  private restoringSig = signal<boolean>(false);
  readonly restoring = this.restoringSig.asReadonly();

  // Restauración en curso compartida: un único /auth/me reutilizado por el guard y el arranque.
  private restore$: Observable<boolean> | null = null;

  constructor() {
    // Si hay token guardado, arrancamos la restauración de sesión apenas carga la app.
    if (this.getToken()) {
      this.restoringSig.set(true);
      this.ensureRestored().subscribe();
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

  /**
   * Garantiza que la sesión esté restaurada antes de activar rutas protegidas.
   * - Sin token → false (el guard redirige a /login).
   * - Usuario ya cargado → true.
   * - Con token → valida contra /auth/me: 200 → true; 401 → limpia sesión y false;
   *   error transitorio (red/500/…) → true (se conserva el token, NO se expulsa al usuario).
   * Reutiliza el request en vuelo (shareReplay) para no llamar /auth/me varias veces.
   */
  ensureRestored(): Observable<boolean> {
    if (this.isAuthenticated()) return of(true);
    if (!this.getToken()) return of(false);

    if (!this.restore$) {
      this.restoringSig.set(true);
      this.restore$ = this.loadCurrentUser().pipe(
        map(() => true),
        catchError((err: HttpErrorResponse) => {
          if (err.status === 401) {
            // Token inválido o expirado: única situación en la que cerramos sesión al refrescar.
            this.clearSession();
            return of(false);
          }
          // Error transitorio: mantenemos la sesión y dejamos pasar (no redirigir a /login).
          return of(true);
        }),
        finalize(() => {
          this.restoringSig.set(false);
          this.restore$ = null;
        }),
        shareReplay(1)
      );
    }
    return this.restore$;
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
