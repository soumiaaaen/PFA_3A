import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface AuditEvent {
  id: string;
  occurredAt: string;
  actorUserId: string | null;
  actorEmail: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  details: string | null;
}

/** Lecture du journal d'audit (réservé à l'administrateur). */
@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);

  list(limit: number, entityType?: string): Observable<AuditEvent[]> {
    let params = new HttpParams().set('limit', limit);
    if (entityType) params = params.set('entityType', entityType);
    return this.http.get<AuditEvent[]>('/api/audit', { params });
  }
}