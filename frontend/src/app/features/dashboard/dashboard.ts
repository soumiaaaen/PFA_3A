import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ROLE_LABELS } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { visibleSections } from '../../core/layout/navigation';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, Icon],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  protected readonly user = inject(AuthService).currentUser;
  protected readonly roleLabels = computed(() => (this.user()?.roles ?? []).map(r => ROLE_LABELS[r]));

  /** Raccourcis : toutes les pages du menu accessibles à l'utilisateur, sauf l'accueil */
  protected readonly shortcuts = computed(() =>
    visibleSections(this.user()?.roles ?? [])
      .flatMap(s => s.items)
      .filter(item => item.path !== '/'),
  );

  /** Horloge en direct */
  protected readonly now = signal(new Date());
  protected readonly time = computed(() =>
    this.now().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
  );
  protected readonly date = computed(() =>
    this.now().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }),
  );
  protected readonly greeting = computed(() => (this.now().getHours() < 18 ? 'Bonjour' : 'Bonsoir'));

  constructor() {
    const timer = setInterval(() => this.now.set(new Date()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }
}