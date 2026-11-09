import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type ActivityType = 'CM' | 'TD' | 'TP';

export interface Assignment {
  id: string;
  elementId: string;
  elementCode: string;
  elementName: string;
  moduleCode: string;
  teacherUserId: string;
  teacherName: string;
  groupId: string;
  groupName: string;
  periodId: string;
  periodNumber: number;
  activityType: ActivityType;
  targetHours: number;
  active: boolean;
}

export interface AssignmentHistory {
  action: 'CREATED' | 'UPDATED' | 'DEACTIVATED';
  oldTeacherUserId: string | null;
  newTeacherUserId: string | null;
  oldTargetHours: number | null;
  newTargetHours: number | null;
  changedBy: string;
  changedAt: string;
}

export interface CreateAssignmentPayload {
  elementId: string;
  teacherUserId: string;
  groupId: string;
  activityType: ActivityType;
  targetHours: number;
}

/** Appels à l'API des affectations (enseignant → élément → groupe). */
@Injectable({ providedIn: 'root' })
export class AssignmentsService {
  private readonly http = inject(HttpClient);

  list(periodId: string): Observable<Assignment[]> {
    return this.http.get<Assignment[]>('/api/assignments', { params: new HttpParams().set('periodId', periodId) });
  }

  create(payload: CreateAssignmentPayload): Observable<Assignment> {
    return this.http.post<Assignment>('/api/assignments', payload);
  }

  update(id: string, teacherUserId: string, targetHours: number): Observable<Assignment> {
    return this.http.put<Assignment>(`/api/assignments/${id}`, { teacherUserId, targetHours });
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`/api/assignments/${id}`);
  }

  history(id: string): Observable<AssignmentHistory[]> {
    return this.http.get<AssignmentHistory[]>(`/api/assignments/${id}/history`);
  }
}