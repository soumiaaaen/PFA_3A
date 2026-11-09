import { Component, computed, inject, signal } from '@angular/core';
import { Icon, IconName } from '../../shared/icon/icon';
import { AuditEvent, AuditService } from './audit.service';

type Kind = 'create' | 'update' | 'delete' | 'auth' | 'other';

/** Libellés lisibles des types d'objets (les types inconnus restent affichés tels quels) */
const ENTITY_LABELS: Record<string, string> = {
  USER: 'Compte',
  ACADEMIC_YEAR: 'Année',
  SEMESTER: 'Semestre',
  PERIOD: 'Période',
  DEPARTMENT: 'Département',
  PROGRAM: 'Filière',
  GROUP: 'Groupe',
  STUDENT: 'Étudiant',
  ENROLLMENT: 'Inscription',
  MODULE: 'Module',
  ELEMENT: 'Élément',
  ASSIGNMENT: 'Affectation',
};

const KIND_ICONS: Record<Kind, IconName> = {
  create: 'check',
  update: 'clipboard',
  delete: 'close',
  auth: 'lock',
  other: 'shield',
};

@Component({
  selector: 'app-audit',
  imports: [Icon],
  templateUrl: './audit.html',
  styleUrl: './audit.scss',
})
export class Audit {
  private readonly api = inject(AuditService);

  protected readonly events = signal<AuditEvent[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);

  protected readonly limit = signal(100);
  protected readonly entityType = signal('');
  protected readonly kind = signal<Kind | ''>('');
  protected readonly search = signal('');
  protected readonly openId = signal<string | null>(null);

  protected readonly limits = [50, 100, 200, 500];
  protected readonly kinds: { value: Kind; label: string }[] = [
    { value: 'create', label: 'Créations' },
    { value: 'update', label: 'Modifications' },
    { value: 'delete', label: 'Suppressions' },
    { value: 'auth', label: 'Connexions' },
  ];

  /** Types connus + types rencontrés dans le journal */
  private readonly seenTypes = signal<Set<string>>(new Set(Object.keys(ENTITY_LABELS)));
  protected readonly entityTypes = computed(() =>
    [...this.seenTypes()].sort((a, b) => this.entityLabel(a).localeCompare(this.entityLabel(b))),
  );

  protected readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    const kind = this.kind();
    return this.events()
      .filter(e => !kind || this.kindOf(e.action) === kind)
      .filter(
        e =>
          !q ||
          [e.actorEmail, e.action, this.actionLabel(e.action), e.details, e.entityId].some(v => v?.toLowerCase().includes(q)),
      );
  });

  /** Événements regroupés par jour, du plus récent au plus ancien */
  protected readonly days = computed(() => {
    const groups = new Map<string, AuditEvent[]>();
    for (const e of [...this.filtered()].sort((a, b) => b.occurredAt.localeCompare(a.occurredAt))) {
      const key = this.dayKey(e.occurredAt);
      groups.set(key, [...(groups.get(key) ?? []), e]);
    }
    return [...groups.entries()].map(([key, items]) => ({ key, label: this.dayLabel(key), items }));
  });

  protected readonly counts = computed(() => {
    const list = this.events();
    const today = this.dayKey(new Date().toISOString());
    return {
      total: list.length,
      today: list.filter(e => this.dayKey(e.occurredAt) === today).length,
      actors: new Set(list.map(e => e.actorEmail ?? 'système')).size,
    };
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.list(this.limit(), this.entityType() || undefined).subscribe({
      next: events => {
        this.events.set(events);
        this.seenTypes.update(set => new Set([...set, ...events.map(e => e.entityType).filter(Boolean)]));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  // ---------- Filtres ----------
  protected onEntityType(e: Event): void {
    this.entityType.set((e.target as HTMLSelectElement).value);
    this.load();
  }

  protected onLimit(e: Event): void {
    this.limit.set(Number((e.target as HTMLSelectElement).value));
    this.load();
  }

  protected onSearch(e: Event): void {
    this.search.set((e.target as HTMLInputElement).value);
  }

  protected toggle(id: string): void {
    this.openId.set(this.openId() === id ? null : id);
  }

  // ---------- Libellés ----------
  protected kindOf(action: string): Kind {
    const a = action.toUpperCase();
    if (/LOGIN|LOGOUT|PASSWORD|TOKEN/.test(a)) return 'auth';
    if (/DELETE|DEACTIVAT|REMOVE|UNENROLL|DISABLE/.test(a)) return 'delete';
    if (/CREATE|ADD|ENROLL|REGISTER/.test(a)) return 'create';
    if (/UPDATE|CHANGE|ASSIGN|SET|EDIT|ROLE|STATUS|HEAD/.test(a)) return 'update';
    return 'other';
  }

  protected kindIcon(action: string): IconName {
    return KIND_ICONS[this.kindOf(action)];
  }

  /** ASSIGNMENT_UPDATED -> « Modification » (l'objet est affiché à côté) */
  protected actionLabel(action: string): string {
    const a = action.toUpperCase();
    const rules: [RegExp, string][] = [
      [/LOGOUT/, 'Déconnexion'],
      [/LOGIN/, 'Connexion'],
      [/PASSWORD/, 'Mot de passe réinitialisé'],
      [/UNENROLL/, 'Désinscription'],
      [/ENROLL/, 'Inscription'],
      [/ROLE/, 'Changement de rôles'],
      [/HEAD/, 'Désignation du chef'],
      [/STATUS|ACTIVAT/, a.includes('DEACTIVAT') ? 'Désactivation' : 'Changement de statut'],
      [/DELETE|REMOVE/, 'Suppression'],
      [/CREATE|ADD|REGISTER/, 'Création'],
      [/UPDATE|CHANGE|EDIT|SET/, 'Modification'],
    ];
    const match = rules.find(([re]) => re.test(a));
    return match ? match[1] : this.humanize(action);
  }

  protected entityLabel(type: string): string {
    return ENTITY_LABELS[type?.toUpperCase()] ?? this.humanize(type ?? '');
  }

  private humanize(code: string): string {
    const text = code.replace(/[_.-]+/g, ' ').toLowerCase().trim();
    return text.charAt(0).toUpperCase() + text.slice(1);
  }

  protected time(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  protected fullDate(iso: string): string {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'full', timeStyle: 'medium' });
  }

  /** Détails : JSON mis en forme si possible, sinon texte brut */
  protected prettyDetails(details: string | null): string {
    if (!details) return '';
    try {
      return JSON.stringify(JSON.parse(details), null, 2);
    } catch {
      return details;
    }
  }

  protected shortDetails(details: string | null): string {
    if (!details) return '';
    const flat = details.replace(/[{}"\n]/g, ' ').replace(/\s+/g, ' ').trim();
    return flat.length > 110 ? flat.slice(0, 110) + '…' : flat;
  }

  private dayKey(iso: string): string {
    const d = new Date(iso);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private dayLabel(key: string): string {
    const now = new Date();
    const today = this.dayKey(now.toISOString());
    const yesterday = this.dayKey(new Date(now.getTime() - 86_400_000).toISOString());
    if (key === today) return "Aujourd'hui";
    if (key === yesterday) return 'Hier';
    const [y, m, d] = key.split('-').map(Number);
    const label = new Date(y, m - 1, d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
}