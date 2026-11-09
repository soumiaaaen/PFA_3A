import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const circle = (cx: number, cy: number, r: number) =>
  `M${cx - r} ${cy}a${r} ${r} 0 1 0 ${2 * r} 0a${r} ${r} 0 1 0 ${-2 * r} 0`;

const rect = (x: number, y: number, w: number, h: number, r: number) =>
  `M${x + r} ${y}h${w - 2 * r}a${r} ${r} 0 0 1 ${r} ${r}v${h - 2 * r}a${r} ${r} 0 0 1 ${-r} ${r}` +
  `h${-(w - 2 * r)}a${r} ${r} 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a${r} ${r} 0 0 1 ${r} ${-r}z`;

const ICONS = {
  mail: [rect(3, 5, 18, 14, 2), 'm3 7 9 6 9-6'],
  lock: [rect(4, 11, 16, 10, 2), 'M8 11V7a4 4 0 0 1 8 0v4'],
  eye: ['M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z', circle(12, 12, 3)],
  'eye-off': [
    'M3 3l18 18',
    'M10.6 5.1A10.9 10.9 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-3.2 4.2',
    'M6.6 6.6C3.9 8.4 2 12 2 12s3.5 7 10 7c1.8 0 3.4-.5 4.8-1.3',
    'M9.9 9.9a3 3 0 0 0 4.2 4.2',
  ],
  alert: [circle(12, 12, 9), 'M12 8v5', 'M12 16h.01'],
  check: ['m5 12 4.5 4.5L19 7'],
  calendar: [rect(3, 5, 18, 16, 2), 'M16 3v4', 'M8 3v4', 'M3 10h18'],
  clock: [circle(12, 12, 9), 'M12 7v5l3 2'],
  users: [circle(9, 8, 3.5), 'M2.5 20a6.5 6.5 0 0 1 13 0', 'M16 4.5a3.5 3.5 0 0 1 0 7', 'M18 14.5a6.5 6.5 0 0 1 3.5 5.5'],
  user: [circle(12, 8, 4), 'M4 21a8 8 0 0 1 16 0'],
  book: ['M4 4h11a3 3 0 0 1 3 3v13H7a3 3 0 0 1-3-3z', 'M4 17a3 3 0 0 1 3-3h11'],
  clipboard: [rect(5, 4, 14, 17, 2), 'M9 3h6v3H9z', 'M9 12h6', 'M9 16h4'],
  folder: ['M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'],
  sparkles: ['M12 3l1.8 4.7 4.7 1.8-4.7 1.8L12 16l-1.8-4.7-4.7-1.8 4.7-1.8z', 'M19 15l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z'],
  home: ['M3 11 12 4l9 7', 'M5 10v10h14V10'],
  layers: ['m12 3 9 5-9 5-9-5z', 'm3 13 9 5 9-5'],
  shield: ['M12 3 5 6v6c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V6z'],
  logout: ['M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3', 'M10 17l-5-5 5-5', 'M5 12h11'],
  menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
  close: ['M6 6l12 12', 'M18 6 6 18'],
  'arrow-right': ['M5 12h14', 'M13 6l6 6-6 6'],
  'chevron-down': ['m6 9 6 6 6-6'],
} satisfies Record<string, string[]>;

export type IconName = keyof typeof ICONS;

/** Icônes SVG simples, sans dépendance externe : <app-icon name="mail" /> */
@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 24 24" [attr.width]="size()" [attr.height]="size()" fill="none" stroke="currentColor"
         stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      @for (d of paths(); track $index) {
        <path [attr.d]="d" />
      }
    </svg>
  `,
  styles: `:host { display: inline-flex; line-height: 0; }`,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly size = input(20);
  protected readonly paths = computed(() => ICONS[this.name()]);
}