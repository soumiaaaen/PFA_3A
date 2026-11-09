import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Department {
  id: string;
  code: string;
  name: string;
  headUserId: string | null;
}

export interface Program {
  id: string;
  departmentId: string;
  departmentCode: string;
  code: string;
  name: string;
  headUserId: string | null;
}

export interface Group {
  id: string;
  programId: string;
  programCode: string;
  academicYearId: string;
  academicYearLabel: string;
  studyLevel: number;
  name: string;
}

export interface Enrollment {
  studentUserId: string;
  studentNumber: string;
  studentName: string;
  groupId: string;
  groupName: string;
}

export interface CreateDepartmentPayload {
  code: string;
  name: string;
}

export interface CreateProgramPayload {
  departmentId: string;
  code: string;
  name: string;
}

export interface CreateGroupPayload {
  programId: string;
  academicYearId: string;
  studyLevel: number;
  name: string;
}

/** Appels à l'API d'organisation : départements, filières, groupes. */
@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly http = inject(HttpClient);

  departments(): Observable<Department[]> {
    return this.http.get<Department[]>('/api/departments');
  }

  createDepartment(payload: CreateDepartmentPayload): Observable<Department> {
    return this.http.post<Department>('/api/departments', payload);
  }

  setDepartmentHead(id: string, userId: string): Observable<Department> {
    return this.http.put<Department>(`/api/departments/${id}/head`, { userId });
  }

  programs(): Observable<Program[]> {
    return this.http.get<Program[]>('/api/programs');
  }

  createProgram(payload: CreateProgramPayload): Observable<Program> {
    return this.http.post<Program>('/api/programs', payload);
  }

  setProgramHead(id: string, userId: string): Observable<Program> {
    return this.http.put<Program>(`/api/programs/${id}/head`, { userId });
  }

  groups(programId: string, academicYearId: string): Observable<Group[]> {
    const params = new HttpParams().set('programId', programId).set('academicYearId', academicYearId);
    return this.http.get<Group[]>('/api/groups', { params });
  }

  createGroup(payload: CreateGroupPayload): Observable<Group> {
    return this.http.post<Group>('/api/groups', payload);
  }

  groupStudents(groupId: string): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`/api/groups/${groupId}/students`);
  }
}