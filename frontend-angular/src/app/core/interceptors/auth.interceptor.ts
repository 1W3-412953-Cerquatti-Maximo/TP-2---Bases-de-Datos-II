import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Agrega el header Authorization: Bearer <token> cuando hay token en localStorage.
 * Los endpoints públicos simplemente lo ignoran.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('nv_token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
