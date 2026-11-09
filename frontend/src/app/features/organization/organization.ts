import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { Icon } from '../../shared/icon/icon';
import { AccountsService, UserAccount } from '../accounts/accounts.service';
import { AcademicYear, CalendarService } from '../calendar/calendar.service';
import { Department, Group, OrganizationService, Program } from './organization.service';

type Tab = 'departments' | 'programs' | 'groups';
type Modal = 'department' | 'program' | 'group' | 'head';

interface HeadTarget {
  kind: 'department' | 'program';
  id: string;
  label: string;
  currentUserId: string | null;
}

@Component({
  selector: 'app-organization',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  templateUrl: './organization.html',
  styleUrl: './organization.scss',
})
export class Organization {
  private readonly api = inject(OrganizationService);
  private readonly accounts = inject(AccountsService);
  private readonly calendar = inject(CalendarService);
  private readonly fb = inject(FormBuilder).nonNullable;

  private readonly today = new Date().toISOString().slice(0, 10);

  // ---------- Données ----------
  protected readonly departments = signal<Department[]>([]);
  protected readonly programs = signal<Program[]>([]);
  protected readonly users = signal<UserAccount[]>([]);
  protected readonly years = signal<AcademicYear[]>([]);
  protected readonly groups = signal<Group[]>([]);
  protected readonly studentCounts = signal<Map<string, number | null>>(new Map());

  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly groupsLoading = signal(false);

  // ---------- Affichage ----------
  protected readonly tab = signal<Tab>('departments');
  protected readonly search = signal('');
  protected readonly departmentFilter = signal('');
  protected readonly programFilter = signal('');
  protected readonly yearId = signal('');

  protected readonly levels = [1, 2, 3];

  private readonly userById = computed(() => new Map(this.users().map(u => [u.id, u])));
  private readonly departmentById = computed(() => new Map(this.departments().map(d => [d.id, d])));

  protected readonly sortedYears = computed(() =>
    [...this.years()].sort((a, b) => b.startDate.localeCompare(a.startDate)),
  );
  protected readonly selectedYear = computed(() => this.years().find(y => y.id === this.yearId()) ?? null);

  protected readonly sortedDepartments = computed(() =>
    [...this.departments()].sort((a, b) => a.code.localeCompare(b.code)),
  );
  protected readonly sortedPrograms = computed(() =>
    [...this.programs()].sort((a, b) => a.code.localeCompare(b.code)),
  );

  // Compteurs des cartes
  protected readonly departmentsWithoutHead = computed(() => this.departments().filter(d => !d.headUserId).length);
  protected readonly programsWithoutHead = computed(() => this.programs().filter(p => !p.headUserId).length);

  // Listes filtrées
  protected readonly filteredDepartments = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.sortedDepartments().filter(d => !q || this.matches(q, d.code, d.name, this.headName(d.headUserId)));
  });

  protected readonly filteredPrograms = computed(() => {
    const q = this.search().trim().toLowerCase();
    const dept = this.departmentFilter();
    return this.sortedPrograms().filter(
      p => (!dept || p.departmentId === dept) && (!q || this.matches(q, p.code, p.name, this.headName(p.headUserId))),
    );
  });

  protected readonly filteredGroups = computed(() => {
    const q = this.search().trim().toLowerCase();
    const prog = this.programFilter();
    return [...this.groups()]
      .filter(g => (!prog || g.programId === prog) && (!q || this.matches(q, g.name, g.programCode)))
      .sort((a, b) => a.programCode.localeCompare(b.programCode) || a.studyLevel - b.studyLevel || a.name.localeCompare(b.name));
  });

  // ---------- Fenêtres ----------
  protected readonly modal = signal<Modal | null>(null);
  protected readonly headTarget = signal<HeadTarget | null>(null);
  protected readonly saving = signal(false);
  protected readonly modalError = signal<string | null>(null);
  protected readonly highlightId = signal<string | null>(null);
  protected readonly toast = signal<{ type: 'ok' | 'ko'; text: string } | null>(null);
  private toastTimer?: ReturnType<typeof setTimeout>;

  protected readonly departmentForm = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(20)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
  });

  protected readonly programForm = this.fb.group({
    departmentId: ['', Validators.required],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
  });

  protected readonly groupForm = this.fb.group({
    programId: ['', Validators.required],
    academicYearId: ['', Validators.required],
    studyLevel: [1, Validators.required],
    name: ['', [Validators.required, Validators.maxLength(50)]],
  });

  protected readonly headForm = this.fb.group({
    userId: ['', Validators.required],
  });

  /** Comptes proposés comme chef : d'abord ceux qui ont le bon rôle, puis les autres enseignants */
  protected readonly headCandidates = computed(() => {
    const target = this.headTarget();
    const role = target?.kind === 'department' ? 'CHEF_DEPARTEMENT' : 'CHEF_FILIERE';
    const active = this.users()
      .filter(u => u.active)
      .sort((a, b) => a.lastName.localeCompare(b.lastName));
    return {
      role,
      withRole: active.filter(u => u.roles.includes(role)),
      teachers: active.filter(u => !u.roles.includes(role) && u.roles.includes('ENSEIGNANT')),
    };
  });

  constructor() {
    this.load();
  }

  // ---------- Chargement ----------
  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    forkJoin({
      departments: this.api.departments(),
      programs: this.api.programs(),
      users: this.accounts.list(),
      years: this.calendar.years(),
    }).subscribe({
      next: data => {
        this.departments.set(data.departments);
        this.programs.set(data.programs);
        this.users.set(data.users);
        this.years.set(data.years);
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

  /** Groupes de toutes les filières pour l'année choisie, avec le nombre d'étudiants */
  protected loadGroups(): void {
    const yearId = this.yearId();
    const programs = this.programs();
    if (!yearId || !programs.length) {
      this.groups.set([]);
      return;
    }
    this.groupsLoading.set(true);
    forkJoin(programs.map(p => this.api.groups(p.id, yearId).pipe(catchError(() => of([] as Group[])))))
      .pipe(map(lists => lists.flat()))
      .subscribe(groups => {
        this.groups.set(groups);
        this.groupsLoading.set(false);
        this.loadCounts(groups);
      });
  }

  private loadCounts(groups: Group[]): void {
    if (!groups.length) {
      this.studentCounts.set(new Map());
      return;
    }
    forkJoin(
      groups.map(g =>
        this.api.groupStudents(g.id).pipe(
          map(list => [g.id, list.length] as const),
          catchError(() => of([g.id, null] as const)),
        ),
      ),
    ).subscribe(pairs => this.studentCounts.set(new Map(pairs)));
  }

  // ---------- Outils d'affichage ----------
  private matches(q: string, ...values: (string | null | undefined)[]): boolean {
    return values.some(v => v?.toLowerCase().includes(q));
  }

  protected head(userId: string | null): UserAccount | null {
    return userId ? this.userById().get(userId) ?? null : null;
  }

  protected headName(userId: string | null): string {
    const u = this.head(userId);
    return u ? `${u.firstName} ${u.lastName}` : '';
  }

  protected initials(u: UserAccount): string {
    return `${u.firstName.charAt(0)}${u.lastName.charAt(0)}`.toUpperCase();
  }

  protected departmentName(id: string): string {
    return this.departmentById().get(id)?.name ?? '';
  }

  protected programCount(departmentId: string): number {
    return this.programs().filter(p => p.departmentId === departmentId).length;
  }

  protected groupCount(programId: string): number {
    return this.groups().filter(g => g.programId === programId).length;
  }

  protected studentCount(groupId: string): number | null | undefined {
    return this.studentCounts().get(groupId);
  }

  protected levelLabel(level: number): string {
    return level === 1 ? '1re année' : `${level}e année`;
  }

  protected setTab(tab: Tab): void {
    this.tab.set(tab);
    this.search.set('');
  }

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  protected onDepartmentFilter(event: Event): void {
    this.departmentFilter.set((event.target as HTMLSelectElement).value);
  }

  protected onProgramFilter(event: Event): void {
    this.programFilter.set((event.target as HTMLSelectElement).value);
  }

  protected onYearChange(event: Event): void {
    this.yearId.set((event.target as HTMLSelectElement).value);
    this.loadGroups();
  }

  // ---------- Ouverture des fenêtres ----------
  protected openCreate(): void {
    this.modalError.set(null);
    const tab = this.tab();
    if (tab === 'departments') {
      this.departmentForm.reset();
      this.modal.set('department');
    } else if (tab === 'programs') {
      this.programForm.reset({ departmentId: this.departmentFilter() || this.sortedDepartments()[0]?.id || '' });
      this.modal.set('program');
    } else {
      this.groupForm.reset({
        programId: this.programFilter() || this.sortedPrograms()[0]?.id || '',
        academicYearId: this.yearId(),
        studyLevel: 1,
        name: '',
      });
      this.modal.set('group');
    }
  }

  protected openHead(kind: 'department' | 'program', item: Department | Program): void {
    this.headTarget.set({ kind, id: item.id, label: `${item.code} · ${item.name}`, currentUserId: item.headUserId });
    this.headForm.reset({ userId: item.headUserId ?? '' });
    this.modalError.set(null);
    this.modal.set('head');
  }

  @HostListener('document:keydown.escape')
  protected closeModal(): void {
    if (!this.saving()) {
      this.modal.set(null);
    }
  }

  // ---------- Enregistrements ----------
  protected submitDepartment(): void {
    if (this.departmentForm.invalid) {
      this.departmentForm.markAllAsTouched();
      return;
    }
    const v = this.departmentForm.getRawValue();
    this.saving.set(true);
    this.api.createDepartment({ code: v.code.trim().toUpperCase(), name: v.name.trim() }).subscribe({
      next: d => {
        this.departments.update(list => [...list, d]);
        this.done(`Département ${d.code} créé`, d.id);
      },
      error: err => this.fail(err),
    });
  }

  protected submitProgram(): void {
    if (this.programForm.invalid) {
      this.programForm.markAllAsTouched();
      return;
    }
    const v = this.programForm.getRawValue();
    this.saving.set(true);
    this.api.createProgram({ departmentId: v.departmentId, code: v.code.trim().toUpperCase(), name: v.name.trim() }).subscribe({
      next: p => {
        this.programs.update(list => [...list, p]);
        this.done(`Filière ${p.code} créée`, p.id);
      },
      error: err => this.fail(err),
    });
  }

  protected submitGroup(): void {
    if (this.groupForm.invalid) {
      this.groupForm.markAllAsTouched();
      return;
    }
    const v = this.groupForm.getRawValue();
    this.saving.set(true);
    this.api.createGroup({ ...v, name: v.name.trim().toUpperCase() }).subscribe({
      next: g => {
        if (g.academicYearId === this.yearId()) {
          this.groups.update(list => [...list, g]);
          this.studentCounts.update(m => new Map(m).set(g.id, 0));
        }
        this.done(`Groupe ${g.name} (${g.programCode}) créé`, g.id);
      },
      error: err => this.fail(err),
    });
  }

  protected submitHead(): void {
    const target = this.headTarget();
    if (!target || this.headForm.invalid) {
      this.headForm.markAllAsTouched();
      return;
    }
    const userId = this.headForm.getRawValue().userId;
    this.saving.set(true);
    const name = this.headName(userId);
    if (target.kind === 'department') {
      this.api.setDepartmentHead(target.id, userId).subscribe({
        next: d => {
          this.departments.update(list => list.map(x => (x.id === d.id ? d : x)));
          this.done(`${name} est chef du département ${d.code}`, d.id);
        },
        error: err => this.fail(err),
      });
    } else {
      this.api.setProgramHead(target.id, userId).subscribe({
        next: p => {
          this.programs.update(list => list.map(x => (x.id === p.id ? p : x)));
          this.done(`${name} est chef de la filière ${p.code}`, p.id);
        },
        error: err => this.fail(err),
      });
    }
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