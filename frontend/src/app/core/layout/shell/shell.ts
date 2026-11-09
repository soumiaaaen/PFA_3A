import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { ROLE_LABELS } from '../../auth/auth.models';
import { AuthService } from '../../auth/auth.service';
import { Icon } from '../../../shared/icon/icon';
import { visibleSections } from '../navigation';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly auth = inject(AuthService);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Menu ouvert sur téléphone */
  protected readonly sidebarOpen = signal(false);
  /** Menu réduit aux icônes sur ordinateur */
  protected readonly collapsed = signal(localStorage.getItem('ensias.sidebar') === 'collapsed');
  /** Menu utilisateur (avatar) */
  protected readonly menuOpen = signal(false);

  protected readonly user = this.auth.currentUser;
  protected readonly sections = computed(() => visibleSections(this.user()?.roles ?? []));
  protected readonly roleLabels = computed(() => (this.user()?.roles ?? []).map(r => ROLE_LABELS[r]));
  protected readonly initials = computed(() => {
    const u = this.user();
    return u ? (u.firstName[0] + u.lastName[0]).toUpperCase() : '';
  });

  constructor() {
    // Ferme les menus à chaque changement de page
    inject(Router).events
      .pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => {
        this.sidebarOpen.set(false);
        this.menuOpen.set(false);
      });
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  protected toggleCollapsed(): void {
    this.collapsed.update(v => !v);
    localStorage.setItem('ensias.sidebar', this.collapsed() ? 'collapsed' : 'expanded');
  }

  protected toggleMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.menuOpen.update(v => !v);
  }

  @HostListener('document:click', ['$event'])
  protected closeMenuOnOutsideClick(event: MouseEvent): void {
    const menu = this.host.nativeElement.querySelector('.user-menu');
    if (this.menuOpen() && menu && !menu.contains(event.target as Node)) {
      this.menuOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  protected closeOnEscape(): void {
    this.menuOpen.set(false);
    this.sidebarOpen.set(false);
  }
}