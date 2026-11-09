import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { Icon } from '../../shared/icon/icon';
import { AcademicYear, CalendarService, Semester } from './calendar.service';

type Status = 'past' | 'current' | 'future';

const DAY = 86_400_000;

/** '2026-09-14' -> Date locale (sans décalage de fuseau horaire) */
function toDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function toIso(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}

function daysBetween(start: string, end: string): number {
  return Math.round((toDate(end).getTime() - toDate(start).getTime()) / DAY);
}

function addDays(iso: string, n: number): string {
  const date = toDate(iso);
  date.setDate(date.getDate() + n);
  return toIso(date);
}

@Component({
  selector: 'app-calendar',
  imports: [ReactiveFormsModule, Icon],
  templateUrl: './calendar.html',
  styleUrl: './calendar.scss',
})
export class Calendar {
  private readonly api = inject(CalendarService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly today = toIso(new Date());

  // ---------- Années ----------
  protected readonly years = signal<AcademicYear[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly selectedYearId = signal<string | null>(null);

  protected readonly sortedYears = computed(() =>
    [...this.years()].sort((a, b) => b.startDate.localeCompare(a.startDate)),
  );
  /** Onglets : de la plus ancienne à la plus récente */
  protected readonly tabYears = computed(() =>
    [...this.years()].sort((a, b) => a.startDate.localeCompare(b.startDate)),
  );
  protected readonly selectedYear = computed(
    () => this.years().find(y => y.id === this.selectedYearId()) ?? null,
  );

  // ---------- Semestres de l'année choisie ----------
  protected readonly semesters = signal<Semester[]>([]);
  protected readonly semestersLoading = signal(false);
  protected readonly sortedSemesters = computed(() =>
    [...this.semesters()]
      .sort((a, b) => a.number - b.number)
      .map(s => ({ ...s, periods: [...s.periods].sort((a, b) => a.number - b.number) })),
  );
  /** Deux emplacements fixes : S1 et S2 (vide si pas encore créé) */
  protected readonly slots = computed(() =>
    [1, 2].map(number => ({
      number,
      semester: this.sortedSemesters().find(s => s.number === number) ?? null,
    })),
  );

  /** Avancement de l'année choisie, en % */
  protected readonly yearProgress = computed(() => {
    const y = this.selectedYear();
    return y ? this.progress(y.startDate, y.endDate) : 0;
  });

  /** Période en cours (si aujourd'hui tombe dans une période) */
  protected readonly currentPeriod = computed(() => {
    for (const s of this.sortedSemesters()) {
      const period = s.periods.find(p => this.status(p.startDate, p.endDate) === 'current');
      if (period) {
        return { semester: s.number, period };
      }
    }
    return null;
  });

  /** Prochaine date importante : début ou fin de période */
  protected readonly nextMilestone = computed(() => {
    const events: { date: string; label: string }[] = [];
    for (const s of this.sortedSemesters()) {
      for (const p of s.periods) {
        if (p.startDate > this.today) {
          events.push({ date: p.startDate, label: `Début S${s.number} · P${p.number}` });
        }
        if (p.endDate >= this.today) {
          events.push({ date: p.endDate, label: `Fin S${s.number} · P${p.number}` });
        }
      }
    }
    events.sort((a, b) => a.date.localeCompare(b.date));
    const next = events[0];
    return next ? { ...next, days: daysBetween(this.today, next.date) } : null;
  });

  // ---------- Calendrier mensuel ----------
  protected readonly weekDays = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  /** Jour cliqué dans le calendrier (aujourd'hui par défaut) */
  protected readonly selectedDay = signal(toIso(new Date()));
  /** Premier jour du mois affiché, ex. '2026-11-01' */
  protected readonly calMonth = signal(toIso(new Date()).slice(0, 8) + '01');

  protected readonly monthLabel = computed(() => {
    const label = toDate(this.calMonth()).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    return label.charAt(0).toUpperCase() + label.slice(1);
  });

  protected readonly canPrev = computed(() => {
    const y = this.selectedYear();
    return !!y && this.calMonth() > y.startDate.slice(0, 8) + '01';
  });

  protected readonly canNext = computed(() => {
    const y = this.selectedYear();
    return !!y && this.calMonth() < y.endDate.slice(0, 8) + '01';
  });

  /** Semaines du mois (du lundi au dimanche), avec le numéro de semaine académique */
  protected readonly monthWeeks = computed(() => {
    const first = this.calMonth();
    const firstDate = toDate(first);
    const offset = (firstDate.getDay() + 6) % 7; // lundi = 0
    const start = addDays(first, -offset);
    const lastDay = new Date(firstDate.getFullYear(), firstDate.getMonth() + 1, 0);
    const weekCount = Math.ceil((offset + lastDay.getDate()) / 7);

    const year = this.selectedYear();
    // Lundi de la semaine de la rentrée = semaine 1
    const yearMonday = year ? addDays(year.startDate, -((toDate(year.startDate).getDay() + 6) % 7)) : null;

    const periods = this.sortedSemesters().flatMap(s =>
      s.periods.map(p => ({
        ...p,
        label: `Semestre ${s.number} · Période ${p.number}`,
        short: `S${s.number} · P${p.number}`,
      })),
    );

    return Array.from({ length: weekCount }, (_, w) => {
      const monday = addDays(start, w * 7);
      const sunday = addDays(monday, 6);
      const inYear = !!year && sunday >= year.startDate && monday <= year.endDate;
      const number = inYear && yearMonday ? Math.floor(daysBetween(yearMonday, monday) / 7) + 1 : null;

      const days = Array.from({ length: 7 }, (_, i) => {
        const iso = addDays(monday, i);
        const date = toDate(iso);
        const period = periods.find(p => iso >= p.startDate && iso <= p.endDate) ?? null;
        const mark = !!period && (iso === period.startDate || iso === period.endDate);
        return {
          iso,
          day: date.getDate(),
          inMonth: date.getMonth() === firstDate.getMonth(),
          weekend: i >= 5,
          today: iso === this.today,
          period,
          mark,
        };
      });

      return { monday, number, current: this.today >= monday && this.today <= sunday, days };
    });
  });

  /** Informations du jour cliqué */
  protected readonly dayInfo = computed(() => {
    const iso = this.selectedDay();
    const year = this.selectedYear();
    const label = toDate(iso).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
    let week: number | null = null;
    if (year && iso >= year.startDate && iso <= year.endDate) {
      const yearMonday = addDays(year.startDate, -((toDate(year.startDate).getDay() + 6) % 7));
      week = Math.floor(daysBetween(yearMonday, iso) / 7) + 1;
    }
    let period: string | null = null;
    let event: string | null = null;
    for (const s of this.sortedSemesters()) {
      for (const p of s.periods) {
        if (iso >= p.startDate && iso <= p.endDate) {
          period = `Semestre ${s.number} · Période ${p.number}`;
          if (iso === p.startDate) event = `Début de la période ${p.number} du S${s.number}`;
          if (iso === p.endDate) event = `Fin de la période ${p.number} du S${s.number}`;
        }
      }
    }
    return { label: label.charAt(0).toUpperCase() + label.slice(1), week, period, event, inYear: week !== null };
  });

  protected readonly takenNumbers = computed(() => this.semesters().map(s => s.number));
  protected readonly canAddSemester = computed(() => this.semesters().length < 2);

  // ---------- Fenêtres ----------
  protected readonly modal = signal<'year' | 'semester' | null>(null);
  protected readonly saving = signal(false);
  protected readonly modalError = signal<string | null>(null);
  protected readonly highlightId = signal<string | null>(null);
  protected readonly toast = signal<{ type: 'ok' | 'ko'; text: string } | null>(null);
  private toastTimer?: ReturnType<typeof setTimeout>;

  protected readonly yearForm = this.fb.group({
    label: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
  });

  protected readonly semesterForm = this.fb.group({
    number: [1, Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    period1EndDate: ['', Validators.required],
  });

  /** Valeurs du formulaire semestre, pour l'aperçu en direct */
  private readonly semesterValues = toSignal(
    this.semesterForm.valueChanges.pipe(map(() => this.semesterForm.getRawValue())),
    { initialValue: this.semesterForm.getRawValue() },
  );

  /** Aperçu du découpage en deux périodes, avec les mêmes règles que le backend */
  protected readonly preview = computed(() => {
    const v = this.semesterValues();
    const year = this.selectedYear();
    if (!v.startDate || !v.endDate || !v.period1EndDate || !year) {
      return null;
    }
    let error: string | null = null;
    if (v.endDate <= v.startDate) {
      error = 'La fin du semestre doit être après son début.';
    } else if (v.startDate < year.startDate || v.endDate > year.endDate) {
      error = `Le semestre doit être compris dans l'année ${year.label}.`;
    } else if (v.period1EndDate <= v.startDate || v.period1EndDate >= v.endDate) {
      error = 'La fin de la période 1 doit être strictement à l’intérieur du semestre.';
    }
    const p2Start = addDays(v.period1EndDate, 1);
    return {
      error,
      p1: { start: v.startDate, end: v.period1EndDate },
      p2: { start: p2Start, end: v.endDate },
    };
  });

  constructor() {
    this.loadYears();
  }

  // ---------- Chargement ----------
  protected loadYears(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.years().subscribe({
      next: years => {
        this.years.set(years);
        this.loading.set(false);
        if (!this.selectedYearId() && years.length) {
          const current = years.find(y => this.today >= y.startDate && this.today <= y.endDate);
          this.selectYear((current ?? this.sortedYears()[0]).id);
        }
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  protected selectYear(id: string): void {
    this.selectedYearId.set(id);
    // Calendrier : mois d'aujourd'hui si l'année est en cours, sinon premier mois de l'année
    const year = this.years().find(y => y.id === id);
    if (year) {
      const inYear = this.today >= year.startDate && this.today <= year.endDate;
      this.calMonth.set((inYear ? this.today : year.startDate).slice(0, 8) + '01');
    }
    this.semesters.set([]);
    this.semestersLoading.set(true);
    this.api.semesters(id).subscribe({
      next: semesters => {
        this.semesters.set(semesters);
        this.semestersLoading.set(false);
      },
      error: () => {
        this.semestersLoading.set(false);
        this.showToast('ko', 'Impossible de charger les semestres');
      },
    });
  }

  // ---------- Outils d'affichage ----------
  protected status(start: string, end: string): Status {
    if (this.today < start) {
      return 'future';
    }
    return this.today > end ? 'past' : 'current';
  }

  protected statusLabel(start: string, end: string): string {
    return { past: 'Terminée', current: 'En cours', future: 'À venir' }[this.status(start, end)];
  }

  /** Avancement d'aujourd'hui entre deux dates, de 0 à 100 */
  protected progress(start: string, end: string): number {
    if (this.today <= start) {
      return 0;
    }
    if (this.today >= end) {
      return 100;
    }
    return Math.round((daysBetween(start, this.today) / Math.max(daysBetween(start, end), 1)) * 100);
  }

  protected daysLeft(end: string): number {
    return daysBetween(this.today, end);
  }

  protected weeks(start: string, end: string): number {
    return Math.round((daysBetween(start, end) + 1) / 7);
  }

  protected fmt(iso: string): string {
    return toDate(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  protected fmtLong(iso: string): string {
    return toDate(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  protected moveMonth(step: number): void {
    const d = toDate(this.calMonth());
    this.calMonth.set(toIso(new Date(d.getFullYear(), d.getMonth() + step, 1)));
  }

  protected goToday(): void {
    this.calMonth.set(this.today.slice(0, 8) + '01');
  }

  // ---------- Fenêtres ----------
  protected openYear(): void {
    // Propose l'année qui suit la plus récente
    const latest = this.sortedYears()[0];
    const first = latest ? Number(latest.label.slice(0, 4)) + 1 : new Date().getFullYear();
    this.yearForm.setValue({
      label: `${first}-${first + 1}`,
      startDate: `${first}-09-14`,
      endDate: `${first + 1}-07-15`,
    });
    this.modalError.set(null);
    this.modal.set('year');
  }

  protected openSemester(preferred?: number): void {
    const year = this.selectedYear();
    if (!year || !this.canAddSemester()) {
      return;
    }
    const number = preferred ?? (this.takenNumbers().includes(1) ? 2 : 1);
    this.fillSemester(number);
    this.modalError.set(null);
    this.modal.set('semester');
  }

  /** Remplit le formulaire avec des dates habituelles pour S1 ou S2 (modifiables) */
  protected fillSemester(number: number): void {
    const year = this.selectedYear();
    if (!year || this.takenNumbers().includes(number)) {
      return;
    }
    const y1 = Number(year.label.slice(0, 4));
    const y2 = y1 + 1;
    this.semesterForm.setValue(
      number === 1
        ? { number, startDate: year.startDate, endDate: `${y2}-01-31`, period1EndDate: `${y1}-11-22` }
        : { number, startDate: `${y2}-02-01`, endDate: `${y2}-06-30`, period1EndDate: `${y2}-04-11` },
    );
  }

  @HostListener('document:keydown.escape')
  protected closeModal(): void {
    if (!this.saving()) {
      this.modal.set(null);
    }
  }

  // ---------- Enregistrements ----------
  protected submitYear(): void {
    if (this.yearForm.invalid) {
      this.yearForm.markAllAsTouched();
      return;
    }
    const v = this.yearForm.getRawValue();
    if (v.endDate <= v.startDate) {
      this.modalError.set('La date de fin doit être après la date de début.');
      return;
    }
    this.saving.set(true);
    this.api.createYear(v).subscribe({
      next: year => {
        this.years.update(list => [...list, year]);
        this.selectYear(year.id);
        this.done(`Année ${year.label} créée`, year.id);
      },
      error: err => this.fail(err),
    });
  }

  protected submitSemester(): void {
    const year = this.selectedYear();
    if (!year) {
      return;
    }
    if (this.semesterForm.invalid) {
      this.semesterForm.markAllAsTouched();
      return;
    }
    const previewError = this.preview()?.error;
    if (previewError) {
      this.modalError.set(previewError);
      return;
    }
    this.saving.set(true);
    this.api.createSemester(year.id, this.semesterForm.getRawValue()).subscribe({
      next: semester => {
        this.semesters.update(list => [...list, semester]);
        this.done(`Semestre S${semester.number} créé avec ses deux périodes`, semester.id);
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