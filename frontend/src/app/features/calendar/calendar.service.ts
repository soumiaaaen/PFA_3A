import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface AcademicYear {
  id: string;
  label: string;
  startDate: string;
  endDate: string;
}

export interface Period {
  id: string;
  semesterId: string;
  number: number;
  startDate: string;
  endDate: string;
}

export interface Semester {
  id: string;
  academicYearId: string;
  label: string;
  number: number;
  startDate: string;
  endDate: string;
  periods: Period[];
}

export interface CreateYearPayload {
  label: string;
  startDate: string;
  endDate: string;
}

export interface CreateSemesterPayload {
  number: number;
  startDate: string;
  endDate: string;
  period1EndDate: string;
}

/** Appels à l'API du calendrier académique. */
@Injectable({ providedIn: 'root' })
export class CalendarService {
  private readonly http = inject(HttpClient);

  years(): Observable<AcademicYear[]> {
    return this.http.get<AcademicYear[]>('/api/academic-years');
  }

  createYear(payload: CreateYearPayload): Observable<AcademicYear> {
    return this.http.post<AcademicYear>('/api/academic-years', payload);
  }

  semesters(yearId: string): Observable<Semester[]> {
    return this.http.get<Semester[]>(`/api/academic-years/${yearId}/semesters`);
  }

  createSemester(yearId: string, payload: CreateSemesterPayload): Observable<Semester> {
    return this.http.post<Semester>(`/api/academic-years/${yearId}/semesters`, payload);
  }
}