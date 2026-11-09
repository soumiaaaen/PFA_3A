import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { CurrentUser, RoleCode, TokenResponse } from './auth.models';

const ACCESS_KEY = 'ensias.accessToken';
const REFRESH_KEY = 'ensias.refreshToken';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly user = signal<CurrentUser | null>(null);
  readonly currentUser = this.user.asReadonly();
  readonly isLoggedIn = computed(() => this.user() !== null);

  private refreshInProgress: Observable<string> | null = null;

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  login(email: string, password: string): Observable<CurrentUser> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password }).pipe(
      tap(tokens => this.storeTokens(tokens)),
      switchMap(() => this.loadMe()),
    );
  }

  loadMe(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>('/api/auth/me').pipe(tap(user => this.user.set(user)));
  }

  /** Au démarrage de l'application : recharge l'utilisateur si des tokens existent. */
  restoreSession(): Observable<unknown> {
    if (!localStorage.getItem(ACCESS_KEY) && !localStorage.getItem(REFRESH_KEY)) {
      return of(null);
    }
    return this.loadMe().pipe(
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
    );
  }

  /** Échange le refresh token contre une nouvelle paire. Un seul appel même si plusieurs requêtes échouent en même temps. */
  refreshAccessToken(): Observable<string> {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (!refreshToken) {
      return throwError(() => new Error('Aucun refresh token'));
    }
    if (!this.refreshInProgress) {
      this.refreshInProgress = this.http
        .post<TokenResponse>('/api/auth/refresh', { refreshToken })
        .pipe(
          tap(tokens => this.storeTokens(tokens)),
          map(tokens => tokens.accessToken),
          finalize(() => (this.refreshInProgress = null)),
          shareReplay(1),
        );
    }
    return this.refreshInProgress;
  }

  logout(): void {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (refreshToken) {
      this.http.post('/api/auth/logout', { refreshToken }).subscribe({ error: () => {} });
    }
    this.clearSession();
    this.router.navigateByUrl('/login');
  }

  clearSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.user.set(null);
  }

  hasAnyRole(...roles: RoleCode[]): boolean {
    const user = this.user();
    return !!user && roles.some(role => user.roles.includes(role));
  }

  private storeTokens(tokens: TokenResponse): void {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  }
}