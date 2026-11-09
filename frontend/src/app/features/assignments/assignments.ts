import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Icon } from '../../shared/icon/icon';
import { AccountsService, UserAccount } from '../accounts/accounts.service';
import { AcademicYear, CalendarService, Semester } from '../calendar/calendar.service';
import { CatalogService, CourseModule } from '../catalog/catalog.service';
import { Group, OrganizationService, Program } from '../organization/organization.service';
import { ActivityType, Assignment, AssignmentHistory, AssignmentsService } from './assignments.service';

type Modal = 'create' | 'edit' | 'history' | 'remove';

interface PeriodOption {
  id: string;
  number: number;
  semesterNumber: number;
  startDate: string;
  endDate: string;
}

@Component({
  selector: 'app-assignments',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  templateUrl: './assignments.html',
  styleUrl: './assignments.scss',
})
export class Assignments {
  private readonly api = inject(AssignmentsService);
  private readonly org = inject(OrganizationService);
  private readonly catalog = inject(CatalogService);
  private readonly calendar = inject(CalendarService);
  private readonly accounts = inject(AccountsService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly today = new Date().toISOString().slice(0, 10);
  protected readonly types: ActivityType[] = ['CM', 'TD', 'TP'];

  // ---------- Données ----------
  protected readonly programs = signal<Program[]>([]);
  protected readonly years = signal<AcademicYear[]>([]);
  protected readonly users = signal<UserAccount[]>([]);
  protected readonly semesters = signal<Semester[]>([]);
  protected readonly groups = signal<Group[]>([]);
  protected readonly assignments = signal<Assignment[]>([]);
  private readonly modulesByProgram = signal<Map<string, CourseModule[]>>(new Map());

  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly listLoading = signal(false);

  // ---------- Filtres ----------
  protected readonly yearId = signal('');
  protected readonly periodId = signal('');
  protected readonly programFilter = signal('');
  protected readonly teacherFilter = signal('');
  protected readonly typeFilter = signal<ActivityType | ''>('');
  protected readonly search = signal('');

  protected readonly sortedYears = computed(() =>
    [...this.years()].sort((a, b) => b.startDate.localeCompare(a.startDate)),
  );
  protected readonly sortedPrograms = computed(() =>
    [...this.programs()].sort((a, b) => a.code.localeCompare(b.code)),
  );

  protected readonly periods = computed<PeriodOption[]>(() =>
    [...this.semesters()]
      .sort((a, b) => a.number - b.number)
      .flatMap(s =>
        [...s.periods]
          .sort((a, b) => a.number - b.number)
          .map(p => ({ id: p.id, number: p.number, semesterNumber: s.number, startDate: p.startDate, endDate: p.endDate })),
      ),
  );
  protected readonly period = computed(() => this.periods().find(p => p.id === this.periodId()) ?? null);

  private readonly groupById = computed(() => new Map(this.groups().map(g => [g.id, g])));
  private readonly programById = computed(() => new Map(this.programs().map(p => [p.id, p])));

  /** Enseignants : comptes actifs avec le rôle ENSEIGNANT */
  protected readonly teachers = computed(() =>
    this.users()
      .filter(u => u.active && u.roles.includes('ENSEIGNANT'))
      .sort((a, b) => a.lastName.localeCompare(b.lastName)),
  );

  /** Enseignants présents dans la liste (pour le filtre, même sans accès aux comptes) */
  protected readonly teacherOptions = computed(() => {
    const map = new Map<string, string>();
    for (const a of this.assignments()) map.set(a.teacherUserId, a.teacherName);
    return [...map.entries()].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
  });

  /** Choix d'enseignant dans les fenêtres (repli sur ceux déjà affectés si les comptes sont inaccessibles) */
  protected readonly teacherChoices = computed(() =>
    this.teachers().length
      ? this.teachers().map(u => ({ id: u.id, name: `${u.lastName} ${u.firstName}`, email: u.email }))
      : this.teacherOptions().map(t => ({ ...t, email: '' })),
  );

  // ---------- Droits ----------
  private readonly isAdmin = computed(() => !!this.auth.currentUser()?.roles.includes('ADMIN'));
  /** Filières que je peux modifier */
  protected readonly editablePrograms = computed(() => {
    const me = this.auth.currentUser();
    if (!me) return [];
    if (this.isAdmin()) return this.sortedPrograms();
    if (!me.roles.includes('CHEF_FILIERE')) return [];
    return this.sortedPrograms().filter(p => p.headUserId === me.id);
  });

  protected canEditRow(a: Assignment): boolean {
    const programId = this.groupById().get(a.groupId)?.programId;
    return !!programId && this.editablePrograms().some(p => p.id === programId);
  }

  // ---------- Lignes et chiffres ----------
  protected readonly rows = computed(() => {
    const q = this.search().trim().toLowerCase();
    const prog = this.programFilter();
    const teacher = this.teacherFilter();
    const type = this.typeFilter();
    return this.assignments()
      .filter(a => !prog || this.groupById().get(a.groupId)?.programId === prog)
      .filter(a => !teacher || a.teacherUserId === teacher)
      .filter(a => !type || a.activityType === type)
      .filter(
        a => !q || [a.elementCode, a.elementName, a.moduleCode, a.teacherName, a.groupName].some(v => v?.toLowerCase().includes(q)),
      )
      .sort(
        (a, b) =>
          Number(b.active) - Number(a.active) ||
          a.elementCode.localeCompare(b.elementCode) ||
          a.groupName.localeCompare(b.groupName) ||
          this.types.indexOf(a.activityType) - this.types.indexOf(b.activityType),
      );
  });

  protected readonly stats = computed(() => {
    const active = this.rows().filter(a => a.active);
    const hours = (t?: ActivityType) =>
      active.filter(a => !t || a.activityType === t).reduce((n, a) => n + Number(a.targetHours || 0), 0);
    const total = hours();
    return {
      count: active.length,
      total,
      teachers: new Set(active.map(a => a.teacherUserId)).size,
      byType: this.types.map(t => ({ type: t, hours: hours(t), share: total ? (hours(t) / total) * 100 : 0 })),
    };
  });

  // ---------- Fenêtres ----------
  protected readonly modal = signal<Modal | null>(null);
  protected readonly target = signal<Assignment | null>(null);
  protected readonly history = signal<AssignmentHistory[] | null>(null);
  protected readonly saving = signal(false);
  protected readonly modalError = signal<string | null>(null);
  protected readonly highlightId = signal<string | null>(null);
  protected readonly toast = signal<{ type: 'ok' | 'ko'; text: string } | null>(null);
  private toastTimer?: ReturnType<typeof setTimeout>;

  protected readonly createForm = this.fb.group({
    programId: ['', Validators.required],
    elementId: ['', Validators.required],
    groupId: ['', Validators.required],
    activityType: ['CM' as ActivityType, Validators.required],
    teacherUserId: ['', Validators.required],
    targetHours: [20, [Validators.required, Validators.min(1), Validators.max(500)]],
  });

  protected readonly editForm = this.fb.group({
    teacherUserId: ['', Validators.required],
    targetHours: [0, [Validators.required, Validators.min(1), Validators.max(500)]],
  });

  private readonly createProgramId = toSignal(this.createForm.controls.programId.valueChanges, { initialValue: '' });

  /** Éléments de la filière choisie, enseignés pendant la période affichée */
  protected readonly createElements = computed(() => {
    const programId = this.createProgramId();
    const periodId = this.periodId();
    return (this.modulesByProgram().get(programId) ?? [])
      .flatMap(m => m.elements.filter(e => e.periodId === periodId).map(e => ({ ...e, moduleName: m.name, level: m.studyLevel })))
      .sort((a, b) => a.code.localeCompare(b.code));
  });

  protected readonly createGroups = computed(() =>
    this.groups()
      .filter(g => g.programId === this.createProgramId())
      .sort((a, b) => a.studyLevel - b.studyLevel || a.name.localeCompare(b.name)),
  );

  constructor() {
    this.load();
  }

  // ---------- Chargement ----------
  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    forkJoin({
      programs: this.org.programs(),
      years: this.calendar.years(),
      // La liste des comptes est réservée à l'administrateur
      users: this.accounts.list().pipe(catchError(() => of([] as UserAccount[]))),
    }).subscribe({
      next: data => {
        this.programs.set(data.programs);
        this.years.set(data.years);
        this.users.set(data.users);
        this.loading.set(false);
        const mine = this.editablePrograms();
        if (!this.isAdmin() && mine.length === 1) this.programFilter.set(mine[0].id);
        const current = data.years.find(y => this.today >= y.startDate && this.today <= y.endDate);
        const year = current ?? this.sortedYears()[0];
        if (year) this.selectYear(year.id);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  private selectYear(id: string): void {
    this.yearId.set(id);
    this.assignments.set([]);
    const programs = this.programs();
    forkJoin({
      semesters: this.calendar.semesters(id).pipe(catchError(() => of([] as Semester[]))),
      groups: programs.length
        ? forkJoin(programs.map(p => this.org.groups(p.id, id).pipe(catchError(() => of([] as Group[]))))).pipe(
            map(lists => lists.flat()),
          )
        : of([] as Group[]),
    }).subscribe(({ semesters, groups }) => {
      this.semesters.set(semesters);
      this.groups.set(groups);
      const periods = this.periods();
      const current = periods.find(p => this.today >= p.startDate && this.today <= p.endDate);
      const period = current ?? periods[0];
      if (period) {
        this.selectPeriod(period.id);
      } else {
        this.periodId.set('');
      }
    });
  }

  protected selectPeriod(id: string): void {
    this.periodId.set(id);
    this.listLoading.set(true);
    this.api.list(id).subscribe({
      next: list => {
        this.assignments.set(list);
        this.listLoading.set(false);
      },
      error: () => {
        this.assignments.set([]);
        this.listLoading.set(false);
        this.showToast('ko', 'Impossible de charger les affectations');
      },
    });
  }

  private loadModules(programId: string): void {
    if (!programId || this.modulesByProgram().has(programId)) return;
    this.catalog.modules(programId).subscribe({
      next: modules => this.modulesByProgram.update(m => new Map(m).set(programId, modules)),
      error: () => this.modulesByProgram.update(m => new Map(m).set(programId, [])),
    });
  }

  // ---------- Événements des filtres ----------
  protected onYearChange(e: Event): void {
    this.selectYear((e.target as HTMLSelectElement).value);
  }

  protected onProgramFilter(e: Event): void {
    this.programFilter.set((e.target as HTMLSelectElement).value);
  }

  protected onTeacherFilter(e: Event): void {
    this.teacherFilter.set((e.target as HTMLSelectElement).value);
  }

  protected onSearch(e: Event): void {
    this.search.set((e.target as HTMLInputElement).value);
  }

  protected onCreateProgram(): void {
    this.loadModules(this.createForm.controls.programId.value);
    this.createForm.patchValue({ elementId: '', groupId: '' });
  }

  // ---------- Outils d'affichage ----------
  protected groupLabel(a: Assignment): string {
    const g = this.groupById().get(a.groupId);
    const p = g ? this.programById().get(g.programId) : null;
    return g ? `${p?.code ?? g.programCode} · ${g.studyLevel}A · ${g.name}` : a.groupName;
  }

  protected initials(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w.charAt(0))
      .join('')
      .toUpperCase();
  }

  protected userName(id: string | null): string {
    if (!id) return '—';
    const u = this.users().find(x => x.id === id);
    if (u) return `${u.firstName} ${u.lastName}`;
    return this.assignments().find(a => a.teacherUserId === id)?.teacherName ?? 'Utilisateur';
  }

  protected fmtDateTime(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  // ---------- Fenêtres ----------
  protected openCreate(): void {
    const programId = this.programFilter() || this.editablePrograms()[0]?.id || '';
    this.createForm.reset({
      programId,
      elementId: '',
      groupId: '',
      activityType: 'CM',
      teacherUserId: '',
      targetHours: 20,
    });
    this.loadModules(programId);
    this.modalError.set(null);
    this.modal.set('create');
  }

  protected openEdit(a: Assignment): void {
    this.target.set(a);
    this.editForm.reset({ teacherUserId: a.teacherUserId, targetHours: Number(a.targetHours) });
    this.modalError.set(null);
    this.modal.set('edit');
  }

  protected openHistory(a: Assignment): void {
    this.target.set(a);
    this.history.set(null);
    this.modalError.set(null);
    this.modal.set('history');
    this.api.history(a.id).subscribe({
      next: h => this.history.set([...h].sort((x, y) => y.changedAt.localeCompare(x.changedAt))),
      error: err => {
        this.history.set([]);
        this.fail(err);
      },
    });
  }

  protected openRemove(a: Assignment): void {
    this.target.set(a);
    this.modalError.set(null);
    this.modal.set('remove');
  }

  @HostListener('document:keydown.escape')
  protected closeModal(): void {
    if (!this.saving()) {
      this.modal.set(null);
    }
  }

  // ---------- Enregistrements ----------
  protected submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const v = this.createForm.getRawValue();
    this.saving.set(true);
    this.api
      .create({
        elementId: v.elementId,
        groupId: v.groupId,
        teacherUserId: v.teacherUserId,
        activityType: v.activityType,
        targetHours: Number(v.targetHours),
      })
      .subscribe({
        next: a => {
          if (a.periodId === this.periodId()) this.assignments.update(list => [...list, a]);
          this.done(`${a.activityType} ${a.elementCode} affecté à ${a.teacherName}`, a.id);
        },
        error: err => this.fail(err),
      });
  }

  protected submitEdit(): void {
    const a = this.target();
    if (!a) return;
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const v = this.editForm.getRawValue();
    this.saving.set(true);
    this.api.update(a.id, v.teacherUserId, Number(v.targetHours)).subscribe({
      next: updated => {
        this.assignments.update(list => list.map(x => (x.id === updated.id ? updated : x)));
        this.done('Affectation modifiée', updated.id);
      },
      error: err => this.fail(err),
    });
  }

  protected submitRemove(): void {
    const a = this.target();
    if (!a) return;
    this.saving.set(true);
    this.api.deactivate(a.id).subscribe({
      next: () => {
        this.assignments.update(list => list.map(x => (x.id === a.id ? { ...x, active: false } : x)));
        this.done(`Affectation ${a.activityType} ${a.elementCode} désactivée`, a.id);
      },
      error: err => this.fail(err),
    });
  }

  private done(message: string, id: string): void {
    this.saving.set(false);
    this.modal.set(null);
    this.showToast('ok', message);
    this.highlightId.set(id);
    setTimeout(() => this.highlightId.set(null), 1800);
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