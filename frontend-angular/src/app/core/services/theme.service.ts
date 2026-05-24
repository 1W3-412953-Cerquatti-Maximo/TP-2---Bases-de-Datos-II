import { Injectable, signal } from '@angular/core';

export type Theme = 'dark' | 'light';

const THEME_KEY = 'nv_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>('dark');

  constructor() {
    const saved = localStorage.getItem(THEME_KEY);
    this.apply(saved === 'light' ? 'light' : 'dark', false);
  }

  set(theme: Theme): void {
    this.apply(theme, true);
  }

  toggle(): void {
    this.apply(this.theme() === 'dark' ? 'light' : 'dark', true);
  }

  private apply(theme: Theme, persist: boolean): void {
    this.theme.set(theme);
    document.documentElement.classList.toggle('theme-light', theme === 'light');
    if (persist) {
      localStorage.setItem(THEME_KEY, theme);
    }
  }
}
