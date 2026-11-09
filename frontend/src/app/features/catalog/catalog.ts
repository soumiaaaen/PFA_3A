import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Icon } from '../../shared/icon/icon';
import { AcademicYear, CalendarService, Semester } from '../calendar/calendar.service';
import { OrganizationService, Program } from '../organization/organization.service';
import { CatalogService, CourseElement, CourseModule } from './catalog.service';

type Modal = 'module' | 'element';

/** Retire les champs vides et convertit le coefficient en nombre */
function clean<T extends { coefficient?: string | number; description?: string; objectives?: string }>(v: T) {
  const coef = String(v.coefficient ?? '').replace(',', '.').trim();
  return {
    ...v,
    description: v.description?.trim() || undefined,
    objectives: v.objectives?.trim() || undefined,
    coefficient: coef ? Number(coef) : undefined,
  };
}

@Component({
  selector: 'app-catalog',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  templateUrl: './catalog.html',
  styleUrl: './catalog.scss',
})
export class Catalog {
  private readonly api = inject(CatalogService);
  private readonly org = inject(OrganizationService);
  private readonly calendar = inject(CalendarService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;

  private readonly today = new Date().toISOString().slice(0, 10);

  // ---------- Données ----------
  protected readonly programs = signal<Program[]>([]);
  protected readonly years = signal<AcademicYear[]>([]);
  protected readonly semesters = signal<Semester[]>([]);
  protected readonly modules = signal<CourseModule[]>([]);

  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly modulesLoading = signal(false);

  // ---------- Affichage ----------
  protected readonly programId = signal('');
  protected readonly yearId = signal('');
  protected readonly level = signal(0); // 0 = tous les niveaux
  protected readonly search = signal('');
  protected readonly openIds = signal<Set<string>>(new Set());
  protected readonly levels = [1, 2, 3];

  protected readonly sortedPrograms = computed(() =>
    [...this.programs()].sort((a, b) => a.code.localeCompare(b.code)),
  );
  protected readonly sortedYears = computed(() =>
    [...this.years()].sort((a, b) => b.startDate.localeCompare(a.startDate)),
  );
  protected readonly program = computed(() => this.programs().find(p => p.id === this.programId()) ?? null);

  /** L'admin modifie tout ; un chef de filière seulement sa filière */
  protected readonly canEdit = computed(() => {
    const me = this.auth.currentUser();
    const program = this.program();
    if (!me || !program) return false;
    return me.roles.includes('ADMIN') || (me.roles.includes('CHEF_FILIERE') && program.headUserId === me.id);
  });

  /** Périodes de l'année choisie, avec leur semestre */
  protected readonly periods = computed(() =>
    [...this.semesters()]
      .sort((a, b) => a.number - b.number)
      .flatMap(s =>
        [...s.periods]
          .sort((a, b) => a.number - b.number)
          .map(p => ({ ...p, semesterNumber: s.number, label: `S${s.number} · Période ${p.number}` })),
      ),
  );
  private readonly periodById = computed(() => new Map(this.periods().map(p => [p.id, p])));

  /** Modules filtrés, regroupés par niveau */
  protected readonly modulesByLevel = computed(() => {
    const q = this.search().trim().toLowerCase();
    const lvl = this.level();
    const list = this.modules()
      .filter(m => !lvl || m.studyLevel === lvl)
      .filter(
        m =>
          !q ||
          [m.code, m.name].some(v => v.toLowerCase().includes(q)) ||
          m.elements.some(e => [e.code, e.name].some(v => v.toLowerCase().includes(q))),
      )
      .sort((a, b) => a.code.localeCompare(b.code));
    return this.levels
      .map(l => ({ level: l, modules: list.filter(m => m.studyLevel === l) }))
      .filter(g => g.modules.length);
  });

  protected readonly elementCount = computed(() => this.modules().reduce((n, m) => n + m.elements.length, 0));

  // ---------- Fenêtres ----------
  protected readonly modal = signal<Modal | null>(null);
  protected readonly targetModule = signal<CourseModule | null>(null);
  protected readonly saving = signal(false);
  protected readonly modalError = signal<string | null>(null);
  protected readonly highlightId = signal<string | null>(null);
  protected readonly toast = signal<{ type: 'ok' | 'ko'; text: string } | null>(null);
  private toastTimer?: ReturnType<typeof setTimeout>;

  protected readonly moduleForm = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    studyLevel: [1, Validators.required],
    coefficient: ['', Validators.pattern(/^\d+([.,]\d+)?$/)],
    description: [''],
    objectives: [''],
  });

  protected readonly elementForm = this.fb.group({
    periodId: ['', Validators.required],
    code: ['', [Validators.required, Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    coefficient: ['', Validators.pattern(/^\d+([.,]\d+)?$/)],
    description: [''],
    objectives: [''],
  });

  constructor() {
    this.load();
  }

  // ---------- Chargement ----------
  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    forkJoin({ programs: this.org.programs(), years: this.calendar.years() }).subscribe({
      next: ({ programs, years }) => {
        this.programs.set(programs);
        this.years.set(years);
        this.loading.set(false);

        // Filière par défaut : celle dont je suis chef, sinon la première
        const me = this.auth.currentUser();
        const mine = programs.find(p => p.headUserId && p.headUserId === me?.id);
        const first = mine ?? this.sortedPrograms()[0];
        if (first) this.selectProgram(first.id);

        const current = years.find(y => this.today >= y.startDate && this.today <= y.endDate);
        const year = current ?? this.sortedYears()[0];
        if (year) this.selectYear(year.id);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  protected selectProgram(id: string): void {
    this.programId.set(id);
    this.openIds.set(new Set());
    this.modulesLoading.set(true);
    this.api.modules(id).subscribe({
      next: modules => {
        this.modules.set(modules);
        this.modulesLoading.set(false);
      },
      error: () => {
        this.modules.set([]);
        this.modulesLoading.set(false);
        this.showToast('ko', 'Impossible de charger les modules');
      },
    });
  }

  private selectYear(id: string): void {
    this.yearId.set(id);
    this.calendar.semesters(id).subscribe({
      next: s => this.semesters.set(s),
      error: () => this.semesters.set([]),
    });
  }

  protected onYearChange(event: Event): void {
    this.selectYear((event.target as HTMLSelectElement).value);
  }

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  protected toggle(id: string): void {
    this.openIds.update(set => {
      const next = new Set(set);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  protected expandAll(): void {
    const all = this.modulesByLevel().flatMap(g => g.modules.map(m => m.id));
    this.openIds.set(this.openIds().size === all.length ? new Set() : new Set(all));
  }

  // ---------- Outils d'affichage ----------
  protected levelLabel(level: number): string {
    return level === 1 ? '1re année' : `${level}e année`;
  }

  protected periodLabel(e: CourseElement): string {
    const p = this.periodById().get(e.periodId);
    return p ? `S${p.semesterNumber} · P${p.number}` : `P${e.periodNumber}`;
  }

  protected sortedElements(m: CourseModule): CourseElement[] {
    return [...m.elements].sort(
      (a, b) => this.periodLabel(a).localeCompare(this.periodLabel(b)) || a.code.localeCompare(b.code),
    );
  }

  // ---------- Fenêtres ----------
  protected openModule(): void {
    const lvl = this.level() || 1;
    this.moduleForm.reset({
      code: `${this.program()?.code ?? ''}${lvl}-M${this.modules().filter(m => m.studyLevel === lvl).length + 1}`,
      name: '',
      studyLevel: lvl,
      coefficient: '',
      description: '',
      objectives: '',
    });
    this.modalError.set(null);
    this.modal.set('module');
  }

  protected openElement(m: CourseModule): void {
    this.targetModule.set(m);
    this.elementForm.reset({
      periodId: this.periods()[0]?.id ?? '',
      code: `${m.code}-E${m.elements.length + 1}`,
      name: '',
      coefficient: '',
      description: '',
      objectives: '',
    });
    this.modalError.set(null);
    this.modal.set('element');
  }

  @HostListener('document:keydown.escape')
  protected closeModal(): void {
    if (!this.saving()) {
      this.modal.set(null);
    }
  }

  // ---------- Enregistrements ----------
  protected submitModule(): void {
    if (this.moduleForm.invalid) {
      this.moduleForm.markAllAsTouched();
      return;
    }
    const v = clean(this.moduleForm.getRawValue());
    this.saving.set(true);
    this.api
      .createModule({ ...v, programId: this.programId(), code: v.code.trim().toUpperCase(), name: v.name.trim() })
      .subscribe({
        next: m => {
          this.modules.update(list => [...list, { ...m, elements: m.elements ?? [] }]);
          this.openIds.update(set => new Set(set).add(m.id));
          this.done(`Module ${m.code} créé`, m.id);
        },
        error: err => this.fail(err),
      });
  }

  protected submitElement(): void {
    const target = this.targetModule();
    if (!target) return;
    if (this.elementForm.invalid) {
      this.elementForm.markAllAsTouched();
      return;
    }
    const v = clean(this.elementForm.getRawValue());
    this.saving.set(true);
    this.api.createElement(target.id, { ...v, code: v.code.trim().toUpperCase(), name: v.name.trim() }).subscribe({
      next: e => {
        this.modules.update(list =>
          list.map(m => (m.id === target.id ? { ...m, elements: [...m.elements, e] } : m)),
        );
        this.done(`Élément ${e.code} ajouté à ${target.code}`, e.id);
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