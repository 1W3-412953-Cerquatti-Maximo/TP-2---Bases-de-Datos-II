import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Protege las rutas dentro del Shell.
 * - Sin token → /login.
 * - Con token y sesión ya restaurada → permite.
 * - Con token pero usuario aún sin cargar (típico al refrescar) → espera la validación
 *   de /auth/me y recién ahí activa la ruta; solo redirige a /login si el token es
 *   inválido o expiró (401). Así no se expulsa por una carrera en el arranque.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.getToken()) return router.createUrlTree(['/login']);
  if (auth.isAuthenticated()) return true;

  return auth.ensureRestored().pipe(
    map(ok => (ok ? true : router.createUrlTree(['/login'])))
  );
};
