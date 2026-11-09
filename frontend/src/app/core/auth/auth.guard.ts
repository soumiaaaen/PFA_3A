import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RoleCode } from './auth.models';
import { AuthService } from './auth.service';

/** Page réservée aux utilisateurs connectés. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/login']);
};

/** Page réservée à certains rôles, ex. canActivate: [authGuard, roleGuard('ADMIN')] */
export function roleGuard(...roles: RoleCode[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.hasAnyRole(...roles) ? true : inject(Router).createUrlTree(['/']);
  };
}