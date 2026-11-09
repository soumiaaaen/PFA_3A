import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Icon } from '../../shared/icon/icon';
import { AccountsService, UserAccount } from '../accounts/accounts.service';
import { AcademicYear, CalendarService } from '../calendar/calendar.service';
import { Group, OrganizationService, Program } from '../organization/organization.service';
import { Enrollment, StudentProfile, StudentsService } from './students.service';

/** 'all' = tous les étudiants, 'none' = sans groupe, sinon l'id d'un groupe */
type Selection = 'all' | 'none' | string;
type Modal = 'profile' | 'enroll' | 'remove';

@Component({
  selector: 'app-students',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  templateUrl: './students.html',
  styleUrl: './students.scss',
})
export class Students {
  private readonly api = inject(StudentsService);
  private readonly org = inject(OrganizationService);
  private readonly accounts = inject(AccountsService);
  private readonly calendar = inject(CalendarService);
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly isAdmin = inject(AuthService).hasAnyRole('ADMIN');

  private readonly today = new Date().toISOString().slice(0, 10);

  // ---------- Données ----------
  protected readonly students = signal<StudentProfile[]>([]);
  protected readonly programs = signal<Program[]>([]);
  protected readonly years = signal<AcademicYear[]>([]);
  protected readonly users = signal<UserAccount[]>([]);
  protected readonly groups = signal<Group[]>([]);
  /** Inscriptions par groupe (pour l'année choisie) */
  protected readonly enrollments = signal<Map<string, Enrollment[]>>(new Map());

  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly groupsLoading = signal(false);

  // ---------- Affichage ----------
  protected readonly yearId = signal('');
  protected readonly selection = signal<Selection>('all');
  protected readonly search = signal('');

  protected readonly sortedYears = computed(() =>
    [...this.years()].sort((a, b) => b.startDate.localeCompare(a.startDate)),
  );

  /** Groupes de l'année, rangés par filière pour le panneau de gauche */
  protected readonly groupsByProgram = computed(() =>
    [...this.programs()]
      .sort((a, b) => a.code.localeCompare(b.code))
      .map(program => ({
        program,
        groups: this.groups()
          .filter(g => g.programId === program.id)
          .sort((a, b) => a.studyLevel - b.studyLevel || a.name.localeCompare(b.name)),
      }))
      .filter(entry => entry.groups.length > 0),
  );

  /** Pour chaque étudiant : ses groupes de l'année */
  private readonly groupsOfStudent = computed(() => {
    const byId = new Map(this.groups().map(g => [g.id, g]));
    const result = new Map<string, Group[]>();
    for (const [groupId, list] of this.enrollments()) {
      const group = byId.get(groupId);
      if (!group) continue;
      for (const e of list) {
        result.set(e.studentUserId, [...(result.get(e.studentUserId) ?? []), group]);
      }
    }
    return result;
  });

  protected readonly withoutGroupCount = computed(
    () => this.students().filter(s => !this.groupsOfStudent().has(s.userId)).length,
  );

  protected readonly selectedGroup = computed(() => {
    const sel = this.selection();
    return sel === 'all' || sel === 'none' ? null : this.groups().find(g => g.id === sel) ?? null;
  });

  /** Lignes du tableau */
  protected readonly rows = computed(() => {
    const sel = this.selection();
    const q = this.search().trim().toLowerCase();
    let list = this.students();
    if (sel === 'none') {
      list = list.filter(s => !this.groupsOfStudent().has(s.userId));
    } else if (sel !== 'all') {
      const ids = new Set((this.enrollments().get(sel) ?? []).map(e => e.studentUserId));
      list = list.filter(s => ids.has(s.userId));
    }
    if (q) {
      list = list.filter(s =>
        [s.firstName, s.lastName, s.email, s.studentNumber].some(v => v?.toLowerCase().includes(q)),
      );
    }
    return [...list].sort((a, b) => a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName));
  });

  // ---------- Fenêtres ----------
  protected readonly modal = signal<Modal | null>(null);
  protected readonly saving = signal(false);
  protected readonly modalError = signal<string | null>(null);
  protected readonly highlightIds = signal<Set<string>>(new Set());
  protected readonly toast = signal<{ type: 'ok' | 'ko'; text: string } | null>(null);
  private toastTimer?: ReturnType<typeof setTimeout>;

  /** Étudiant à retirer du groupe */
  protected readonly toRemove = signal<StudentProfile | null>(null);

  /** Inscription : recherche et étudiants cochés */
  protected readonly enrollSearch = signal('');
  protected readonly picked = signal<Set<string>>(new Set());

  protected readonly enrollCandidates = computed(() => {
    const group = this.selectedGroup();
    if (!group) return [];
    const inGroup = new Set((this.enrollments().get(group.id) ?? []).map(e => e.studentUserId));
    const q = this.enrollSearch().trim().toLowerCase();
    return this.students()
      .filter(s => !inGroup.has(s.userId))
      .filter(s => !q || [s.firstName, s.lastName, s.email, s.studentNumber].some(v => v?.toLowerCase().includes(q)))
      .sort((a, b) => a.lastName.localeCompare(b.lastName));
  });

  /** Comptes ETUDIANT qui n'ont pas encore de profil étudiant */
  protected readonly accountsWithoutProfile = computed(() => {
    const withProfile = new Set(this.students().map(s => s.userId));
    return this.users()
      .filter(u => u.active && u.roles.includes('ETUDIANT') && !withProfile.has(u.id))
      .sort((a, b) => a.lastName.localeCompare(b.lastName));
  });

  protected readonly profileForm = this.fb.group({
    userId: ['', Validators.required],
    studentNumber: ['', [Validators.required, Validators.maxLength(30)]],
  });

  constructor() {
    this.load();
  }

  // ---------- Chargement ----------
  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    forkJoin({
      students: this.api.list(),
      programs: this.org.programs(),
      years: this.calendar.years(),
      // La liste des comptes est réservée à l'administrateur
      users: this.isAdmin ? this.accounts.list().pipe(catchError(() => of([] as UserAccount[]))) : of([] as UserAccount[]),
    }).subscribe({
      next: data => {
        this.students.set(data.students);
        this.programs.set(data.programs);
        this.years.set(data.years);
        this.users.set(data.users);
        this.loading.set(false);
        const current = data.years.find(y => this.today >= y.startDate && this.today <= y.endDate);
        const year = current ?? this.sortedYears()[0];
        if (year) {
          this.yearId.set(year.id);
          this.loadGroups();
        }
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  private loadGroups(): void {
    const yearId = this.yearId();
    const programs = this.programs();
    this.selection.set('all');
    if (!yearId || !programs.length) {
      this.groups.set([]);
      this.enrollments.set(new Map());
      return;
    }
    this.groupsLoading.set(true);
    forkJoin(programs.map(p => this.org.groups(p.id, yearId).pipe(catchError(() => of([] as Group[])))))
      .pipe(map(lists => lists.flat()))
      .subscribe(groups => {
        this.groups.set(groups);
        if (!groups.length) {
          this.enrollments.set(new Map());
          this.groupsLoading.set(false);
          return;
        }
        forkJoin(
          groups.map(g =>
            this.api.groupStudents(g.id).pipe(
              map(list => [g.id, list] as const),
              catchError(() => of([g.id, [] as Enrollment[]] as const)),
            ),
          ),
        ).subscribe(pairs => {
          this.enrollments.set(new Map(pairs));
          this.groupsLoading.set(false);
        });
      });
  }

  private reloadGroup(groupId: string): void {
    this.api.groupStudents(groupId).subscribe(list =>
      this.enrollments.update(m => new Map(m).set(groupId, list)),
    );
  }

  // ---------- Outils d'affichage ----------
  protected groupsOf(userId: string): Group[] {
    return this.groupsOfStudent().get(userId) ?? [];
  }

  protected count(groupId: string): number {
    return this.enrollments().get(groupId)?.length ?? 0;
  }

  protected initials(s: { firstName: string; lastName: string }): string {
    return `${s.firstName.charAt(0)}${s.lastName.charAt(0)}`.toUpperCase();
  }

  protected onYearChange(event: Event): void {
    this.yearId.set((event.target as HTMLSelectElement).value);
    this.loadGroups();
  }

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  protected onEnrollSearch(event: Event): void {
    this.enrollSearch.set((event.target as HTMLInputElement).value);
  }

  protected select(sel: Selection): void {
    this.selection.set(sel);
  }

  // ---------- Fenêtres ----------
  protected openProfile(): void {
    this.profileForm.reset({ userId: this.accountsWithoutProfile()[0]?.id ?? '', studentNumber: '' });
    this.modalError.set(null);
    this.modal.set('profile');
  }

  protected openEnroll(): void {
    this.enrollSearch.set('');
    this.picked.set(new Set());
    this.modalError.set(null);
    this.modal.set('enroll');
  }

  protected openRemove(student: StudentProfile): void {
    this.toRemove.set(student);
    this.modalError.set(null);
    this.modal.set('remove');
  }

  protected togglePick(userId: string): void {
    this.picked.update(set => {
      const next = new Set(set);
      if (next.has(userId)) {
        next.delete(userId);
      } else {
        next.add(userId);
      }
      return next;
    });
  }

  protected toggleAll(): void {
    const all = this.enrollCandidates().map(s => s.userId);
    this.picked.set(this.picked().size === all.length ? new Set() : new Set(all));
  }

  @HostListener('document:keydown.escape')
  protected closeModal(): void {
    if (!this.saving()) {
      this.modal.set(null);
    }
  }

  // ---------- Enregistrements ----------
  protected submitProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    const v = this.profileForm.getRawValue();
    this.saving.set(true);
    this.api.create(v.userId, v.studentNumber.trim().toUpperCase()).subscribe({
      next: s => {
        this.students.update(list => [...list, s]);
        this.done(`Profil étudiant créé pour ${s.firstName} ${s.lastName}`, [s.userId]);
      },
      error: err => this.fail(err),
    });
  }

  protected submitEnroll(): void {
    const group = this.selectedGroup();
    const ids = [...this.picked()];
    if (!group || !ids.length) return;
    this.saving.set(true);
    forkJoin(
      ids.map(id =>
        this.api.enroll(group.id, id).pipe(
          map(() => ({ id, error: null as string | null })),
          catchError((err: HttpErrorResponse) => of({ id, error: (err.error?.detail as string) ?? 'erreur' })),
        ),
      ),
    ).subscribe(results => {
      this.reloadGroup(group.id);
      const ok = results.filter(r => !r.error);
      const ko = results.filter(r => r.error);
      if (!ko.length) {
        this.done(`${ok.length} étudiant${ok.length > 1 ? 's' : ''} inscrit${ok.length > 1 ? 's' : ''} dans ${group.name}`, ok.map(r => r.id));
      } else {
        this.saving.set(false);
        this.picked.set(new Set(ko.map(r => r.id)));
        this.modalError.set(
          `${ok.length} inscrit(s), ${ko.length} refusé(s) : ${[...new Set(ko.map(r => r.error))].join(' ; ')}`,
        );
      }
    });
  }

  protected submitRemove(): void {
    const group = this.selectedGroup();
    const student = this.toRemove();
    if (!group || !student) return;
    this.saving.set(true);
    this.api.unenroll(group.id, student.userId).subscribe({
      next: () => {
        this.enrollments.update(m =>
          new Map(m).set(group.id, (m.get(group.id) ?? []).filter(e => e.studentUserId !== student.userId)),
        );
        this.done(`${student.firstName} ${student.lastName} retiré(e) de ${group.name}`, []);
      },
      error: err => this.fail(err),
    });
  }

  private done(message: string, ids: string[]): void {
    this.saving.set(false);
    this.modal.set(null);
    this.showToast('ok', message);
    this.highlightIds.set(new Set(ids));
    setTimeout(() => this.highlightIds.set(new Set()), 1800);
  }

  private fail(err: HttpErrorResponse): void {
    this.saving.set(false);
    const fieldErrors = err.error?.errors ? Object.values(err.error.errors).join(', ') : null;
    this.modalError.set(fieldErrors ?? err.error?.detail ?? 'Une erreur est survenue, réessayez');
  }

  private showToast(type: 'ok' | 'ko', text: string): void {
    clearTimeout(this.toastTimer);
    this.toast.set({ type, text });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3200);
  }
}