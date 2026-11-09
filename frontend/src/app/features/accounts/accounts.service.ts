import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RoleCode } from '../../core/auth/auth.models';

export interface UserAccount {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  roles: RoleCode[];
}

export interface CreateUserPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  roles: RoleCode[];
}

/** Appels à l'API de gestion des comptes (réservée à l'administrateur). */
@Injectable({ providedIn: 'root' })
export class AccountsService {
  private readonly http = inject(HttpClient);

  list(): Observable<UserAccount[]> {
    return this.http.get<UserAccount[]>('/api/users');
  }

  create(payload: CreateUserPayload): Observable<UserAccount> {
    return this.http.post<UserAccount>('/api/users', payload);
  }

  updateRoles(id: string, roles: RoleCode[]): Observable<UserAccount> {
    return this.http.put<UserAccount>(`/api/users/${id}/roles`, { roles });
  }

  setActive(id: string, active: boolean): Observable<UserAccount> {
    return this.http.patch<UserAccount>(`/api/users/${id}/status`, { active });
  }

  resetPassword(id: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`/api/users/${id}/reset-password`, { newPassword });
  }
}