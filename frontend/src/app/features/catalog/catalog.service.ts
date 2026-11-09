import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CourseElement {
  id: string;
  moduleId: string;
  moduleCode: string;
  periodId: string;
  periodNumber: number;
  code: string;
  name: string;
  description: string | null;
  objectives: string | null;
  coefficient: number | null;
}

export interface CourseModule {
  id: string;
  programId: string;
  programCode: string;
  code: string;
  name: string;
  studyLevel: number;
  description: string | null;
  objectives: string | null;
  coefficient: number | null;
  elements: CourseElement[];
}

export interface CreateModulePayload {
  programId: string;
  code: string;
  name: string;
  studyLevel: number;
  description?: string;
  objectives?: string;
  coefficient?: number;
}

export interface CreateElementPayload {
  periodId: string;
  code: string;
  name: string;
  description?: string;
  objectives?: string;
  coefficient?: number;
}

/** Appels à l'API du catalogue : modules et éléments de module. */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  modules(programId: string): Observable<CourseModule[]> {
    return this.http.get<CourseModule[]>('/api/modules', { params: new HttpParams().set('programId', programId) });
  }

  createModule(payload: CreateModulePayload): Observable<CourseModule> {
    return this.http.post<CourseModule>('/api/modules', payload);
  }

  createElement(moduleId: string, payload: CreateElementPayload): Observable<CourseElement> {
    return this.http.post<CourseElement>(`/api/modules/${moduleId}/elements`, payload);
  }
}