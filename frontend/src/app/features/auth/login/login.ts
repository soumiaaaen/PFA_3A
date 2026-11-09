import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

interface Ripple {
  id: number;
  x: number;
  y: number;
}

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly success = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly shake = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly capsLock = signal(false);
  protected readonly showHelp = signal(false);

  /** Inclinaison 3D du cadre qui suit la souris */
  protected readonly tilt = signal('');
  /** Ondes du bouton au clic */
  protected readonly ripples = signal<Ripple[]>([]);

  /** Mots qui défilent dans le titre */
  private readonly words = ['gestion pédagogique', 'emplois du temps', 'suivi des absences', 'ressources de cours'];
  protected readonly typed = signal('');

  protected readonly year = new Date().getFullYear();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  constructor() {
    this.startTyping();
  }

  protected togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  protected toggleHelp(): void {
    this.showHelp.update(v => !v);
  }

  protected checkCapsLock(event: KeyboardEvent): void {
    this.capsLock.set(event.getModifierState?.('CapsLock') ?? false);
  }

  protected onTilt(event: MouseEvent): void {
    const box = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const x = (event.clientX - box.left) / box.width - 0.5;
    const y = (event.clientY - box.top) / box.height - 0.5;
    this.tilt.set(`perspective(900px) rotateX(${(-y * 5).toFixed(2)}deg) rotateY(${(x * 5).toFixed(2)}deg)`);
  }

  protected resetTilt(): void {
    this.tilt.set('');
  }

  protected addRipple(event: MouseEvent): void {
    const button = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const ripple = { id: Date.now(), x: event.clientX - button.left, y: event.clientY - button.top };
    this.ripples.update(list => [...list, ripple]);
    setTimeout(() => this.ripples.update(list => list.filter(r => r.id !== ripple.id)), 600);
  }

  protected submit(): void {
    if (this.loading() || this.success()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.triggerShake();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        setTimeout(() => this.router.navigateByUrl('/'), 1200);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(
          err.status === 401
            ? (err.error?.detail ?? 'Email ou mot de passe incorrect')
            : 'Service indisponible, réessayez dans un instant',
        );
        this.form.controls.password.reset();
        this.triggerShake();
      },
    });
  }

  private triggerShake(): void {
    this.shake.set(false);
    requestAnimationFrame(() => this.shake.set(true));
    setTimeout(() => this.shake.set(false), 500);
  }

  /** Effet machine à écrire : écrit un mot, attend, l'efface, passe au suivant. */
  private startTyping(): void {
    let word = 0;
    let letters = 0;
    let deleting = false;
    let pause = 0;

    const timer = setInterval(() => {
      if (pause > 0) {
        pause--;
        return;
      }
      const current = this.words[word];
      letters += deleting ? -1 : 1;
      this.typed.set(current.slice(0, letters));

      if (!deleting && letters === current.length) {
        deleting = true;
        pause = 25; // reste affiché ~2 secondes
      } else if (deleting && letters === 0) {
        deleting = false;
        word = (word + 1) % this.words.length;
        pause = 4;
      }
    }, 75);

    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }
}