import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { NAVIGATION } from '../../core/layout/navigation';
import { Icon } from '../../shared/icon/icon';

/** Page provisoire pour les sections pas encore développées. */
@Component({
  selector: 'app-coming-soon',
  imports: [RouterLink, Icon],
  template: `
    <section class="box">
      <span class="icon"><app-icon [name]="item()?.icon ?? 'layers'" [size]="30" /></span>
      <h1>{{ item()?.label ?? 'Page introuvable' }}</h1>
      <p>{{ item() ? 'Cette section est en cours de développement.' : 'Cette adresse ne correspond à aucune page.' }}</p>
      <a routerLink="/" class="back">Retour au tableau de bord</a>
    </section>
  `,
  styles: `
    .box {
      max-width: 520px;
      margin: 40px auto;
      padding: 40px 28px;
      text-align: center;
      border: 1px dashed #d1d5db;
      border-top: 4px solid #dc2626;
      border-radius: 10px;
      animation: rise .4s ease both;
    }
    .icon {
      width: 64px; height: 64px;
      display: inline-grid; place-items: center;
      border-radius: 14px;
      background: #eff6ff;
      color: #2563eb;
      animation: bob 2.4s ease-in-out infinite;
    }
    h1 { margin: 18px 0 6px; font-size: 22px; color: #111827; }
    p { margin: 0 0 24px; color: #6b7280; }
    .back {
      display: inline-block;
      padding: 11px 22px;
      border-radius: 6px;
      background: #2563eb;
      color: #fff;
      text-decoration: none;
      font-weight: 600;
      transition: background .15s ease, transform .15s ease;
    }
    .back:hover { background: #1d4ed8; transform: translateY(-2px); }
    @keyframes rise { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: none; } }
    @keyframes bob { 50% { transform: translateY(-6px); } }
  `,
})
export class ComingSoon {
  private readonly segments = toSignal(inject(ActivatedRoute).url, { initialValue: [] });
  protected readonly item = computed(() => {
    const path = '/' + this.segments().map(s => s.path).join('/');
    return NAVIGATION.flatMap(s => s.items).find(i => i.path === path);
  });
}