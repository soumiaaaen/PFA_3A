import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLIC_URLS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (PUBLIC_URLS.some(url => req.url.startsWith(url))) {
    return next(req);
  }
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(withToken(req, auth.accessToken)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      // Token expiré : on tente un refresh, puis on rejoue la requête
      return auth.refreshAccessToken().pipe(
        catchError(refreshError => {
          auth.clearSession();
          router.navigateByUrl('/login');
          return throwError(() => refreshError);
        }),
        switchMap(newToken => next(withToken(req, newToken))),
      );
    }),
  );
};

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}