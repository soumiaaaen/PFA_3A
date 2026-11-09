import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface StudentProfile {
  userId: string;
  studentNumber: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface Enrollment {
  studentUserId: string;
  studentNumber: string;
  studentName: string;
  groupId: string;
  groupName: string;
  programCode: string;
  studyLevel: number;
  academicYearLabel: string;
}

/** Appels à l'API des étudiants : profils et inscriptions dans les groupes. */
@Injectable({ providedIn: 'root' })
export class StudentsService {
  private readonly http = inject(HttpClient);

  list(): Observable<StudentProfile[]> {
    return this.http.get<StudentProfile[]>('/api/students');
  }

  create(userId: string, studentNumber: string): Observable<StudentProfile> {
    return this.http.post<StudentProfile>('/api/students', { userId, studentNumber });
  }

  groupStudents(groupId: string): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`/api/groups/${groupId}/students`);
  }

  enroll(groupId: string, studentUserId: string): Observable<Enrollment> {
    return this.http.post<Enrollment>(`/api/groups/${groupId}/students`, { studentUserId });
  }

  unenroll(groupId: string, studentUserId: string): Observable<void> {
    return this.http.delete<void>(`/api/groups/${groupId}/students/${studentUserId}`);
  }
}