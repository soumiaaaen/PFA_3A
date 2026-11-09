import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ROLE_LABELS, RoleCode } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { Icon } from '../../shared/icon/icon';
import { AccountsService, UserAccount } from './accounts.service';

type Modal =
  | { kind: 'create' }
  | { kind: 'roles'; user: UserAccount }
  | { kind: 'password'; user: UserAccount }
  | null;

type StatusFilter = 'all' | 'active' | 'inactive';

@Component({
  selector: 'app-accounts',
  imports: [ReactiveFormsModule, Icon],
  templateUrl: './accounts.html',
  styleUrl: './accounts.scss',
})
export class Accounts {
  private readonly api = inject(AccountsService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly roleCodes = Object.keys(ROLE_LABELS) as RoleCode[];
  protected readonly roleLabels = ROLE_LABELS;

  // ---------- Données ----------
  protected readonly users = signal<UserAccount[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly currentUserId = computed(() => this.auth.currentUser()?.id);

  // ---------- Filtres ----------
  protected readonly search = signal('');
  protected readonly roleFilter = signal<RoleCode | ''>('');
  protected readonly statusFilter = signal<StatusFilter>('all');

  private readonly palette = [
    '#2563eb', '#dc2626', '#0d9488', '#7c3aed', '#ea580c', '#0891b2',
    '#db2777', '#16a34a', '#4f46e5', '#b45309', '#0f766e', '#9333ea',
  ];

  /** Comptes triés par email, puis une couleur de la palette chacun, dans l'ordre. */
  private readonly avatarColors = computed(() => {
    const sorted = [...this.users()].sort((a, b) => a.email.localeCompare(b.email));
    return new Map(sorted.map((u, i) => [u.id, this.palette[i % this.palette.length]]));
  });

  protected readonly stats = computed(() => {
    const all = this.users();
    const active = all.filter(u => u.active).length;
    return { total: all.length, active, inactive: all.length - active };
  });

  protected readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    const role = this.roleFilter();
    const status = this.statusFilter();
    return this.users()
      .filter(u => !q || `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(q))
      .filter(u => !role || u.roles.includes(role))
      .filter(u => status === 'all' || (status === 'active') === u.active)
      .sort((a, b) => a.lastName.localeCompare(b.lastName, 'fr'));
  });

  // ---------- Fenêtres (création, rôles, mot de passe) ----------
  protected readonly modal = signal<Modal>(null);
  protected readonly saving = signal(false);
  protected readonly modalError = signal<string | null>(null);
  protected readonly selectedRoles = signal<RoleCode[]>([]);
  protected readonly showPassword = signal(false);
  protected readonly copied = signal(false);

  protected readonly createForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  // ---------- Retours visuels ----------
  protected readonly togglingId = signal<string | null>(null);
  protected readonly highlightId = signal<string | null>(null);
  protected readonly toast = signal<{ type: 'ok' | 'ko'; text: string } | null>(null);
  private toastTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.list().subscribe({
      next: users => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  protected onRoleFilter(event: Event): void {
    this.roleFilter.set((event.target as HTMLSelectElement).value as RoleCode | '');
  }

  protected resetFilters(): void {
    this.search.set('');
    this.roleFilter.set('');
    this.statusFilter.set('all');
  }

  // ---------- Ouverture des fenêtres ----------
  protected openCreate(): void {
    this.createForm.reset();
    this.selectedRoles.set(['ENSEIGNANT']);
    this.openModal({ kind: 'create' });
  }

  protected openRoles(user: UserAccount): void {
    this.selectedRoles.set([...user.roles]);
    this.openModal({ kind: 'roles', user });
  }

  protected openPassword(user: UserAccount): void {
    this.passwordForm.reset();
    this.openModal({ kind: 'password', user });
  }

  private openModal(modal: Modal): void {
    this.modalError.set(null);
    this.showPassword.set(false);
    this.copied.set(false);
    this.modal.set(modal);
  }

  @HostListener('document:keydown.escape')
  protected closeModal(): void {
    if (!this.saving()) {
      this.modal.set(null);
    }
  }

  protected toggleRole(role: RoleCode): void {
    this.selectedRoles.update(list => (list.includes(role) ? list.filter(r => r !== role) : [...list, role]));
    this.modalError.set(null);
  }

  /** Génère un mot de passe aléatoire de 12 caractères (majuscule, minuscule, chiffre, symbole). */
  protected generatePassword(target: 'create' | 'reset'): void {
    const sets = ['ABCDEFGHJKLMNPQRSTUVWXYZ', 'abcdefghijkmnopqrstuvwxyz', '23456789', '@#$%&*!?'];
    const random = (chars: string) => chars[crypto.getRandomValues(new Uint32Array(1))[0] % chars.length];
    const chars = sets.map(random);
    while (chars.length < 12) {
      chars.push(random(sets.join('')));
    }
    const password = chars.sort(() => crypto.getRandomValues(new Uint32Array(1))[0] / 2 ** 32 - 0.5).join('');

    if (target === 'create') {
      this.createForm.controls.password.setValue(password);
    } else {
      this.passwordForm.controls.newPassword.setValue(password);
    }
    this.showPassword.set(true);
    this.copied.set(false);
  }

  protected copyPassword(value: string): void {
    navigator.clipboard?.writeText(value).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 1500);
    });
  }

  // ---------- Enregistrements ----------
  protected submitCreate(): void {
    if (this.createForm.invalid || this.selectedRoles().length === 0) {
      this.createForm.markAllAsTouched();
      if (this.selectedRoles().length === 0) {
        this.modalError.set('Choisissez au moins un rôle');
      }
      return;
    }
    this.saving.set(true);
    this.api.create({ ...this.createForm.getRawValue(), roles: this.selectedRoles() }).subscribe({
      next: user => {
        this.users.update(list => [...list, user]);
        this.done(`Compte de ${user.firstName} ${user.lastName} créé`, user.id);
      },
      error: err => this.fail(err),
    });
  }

  protected submitRoles(): void {
    const modal = this.modal();
    if (modal?.kind !== 'roles') {
      return;
    }
    if (this.selectedRoles().length === 0) {
      this.modalError.set('Choisissez au moins un rôle');
      return;
    }
    this.saving.set(true);
    this.api.updateRoles(modal.user.id, this.selectedRoles()).subscribe({
      next: user => {
        this.replace(user);
        this.done(`Rôles de ${user.firstName} ${user.lastName} mis à jour`, user.id);
      },
      error: err => this.fail(err),
    });
  }

  protected submitPassword(): void {
    const modal = this.modal();
    if (modal?.kind !== 'password') {
      return;
    }
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.api.resetPassword(modal.user.id, this.passwordForm.getRawValue().newPassword).subscribe({
      next: () => this.done(`Mot de passe de ${modal.user.firstName} ${modal.user.lastName} réinitialisé`, modal.user.id),
      error: err => this.fail(err),
    });
  }

  protected toggleActive(user: UserAccount): void {
    if (user.id === this.currentUserId() || this.togglingId()) {
      return;
    }
    this.togglingId.set(user.id);
    this.api.setActive(user.id, !user.active).subscribe({
      next: updated => {
        this.replace(updated);
        this.togglingId.set(null);
        this.showToast('ok', updated.active ? `${updated.firstName} ${updated.lastName} réactivé` : `${updated.firstName} ${updated.lastName} désactivé`);
      },
      error: () => {
        this.togglingId.set(null);
        this.showToast('ko', 'Action impossible, réessayez');
      },
    });
  }

  protected initials(user: UserAccount): string {
    return (user.firstName[0] + user.lastName[0]).toUpperCase();
  }

  /** Couleur d'avatar : chaque compte reçoit une couleur différente de la palette. */
  protected avatarColor(user: UserAccount): string {
    return this.avatarColors().get(user.id) ?? '#2563eb';
  }

  /** Rôles toujours affichés dans le même ordre (du plus élevé au plus courant). */
  protected sortedRoles(user: UserAccount): RoleCode[] {
    return this.roleCodes.filter(r => user.roles.includes(r));
  }

  // ---------- Outils ----------
  private replace(user: UserAccount): void {
    this.users.update(list => list.map(u => (u.id === user.id ? user : u)));
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