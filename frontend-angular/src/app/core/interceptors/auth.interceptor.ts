import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * - Agrega Authorization: Bearer <token> cuando hay token en localStorage.
 * - Si una llamada protegida devuelve 401, limpia la sesión y redirige a /login.
 *   (Se excluyen los endpoints /api/auth/* para no interferir con login/register.)
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('nv_token');
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  const router = inject(Router);
  const auth = inject(AuthService);

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.includes('/api/auth/')) {
        auth.clearSession();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};
