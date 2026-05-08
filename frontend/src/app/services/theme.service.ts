import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

const STORAGE_KEY = 'agenda-theme';

function readStoredDark(): boolean | null {
  if (typeof localStorage === 'undefined') {
    return null;
  }
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw === 'dark') {
    return true;
  }
  if (raw === 'light') {
    return false;
  }
  return null;
}

/** Convertit `#RGB` / `#RRGGBB` en rgba (fallback gris slate si invalide). */
export function hexToRgba(hex: string | undefined | null, alpha: number): string {
  let h = (hex ?? '').trim();
  if (!h.startsWith('#')) {
    h = '#64748b';
  }
  if (h.length === 4) {
    const r = parseInt(h[1] + h[1], 16);
    const g = parseInt(h[2] + h[2], 16);
    const b = parseInt(h[3] + h[3], 16);
    return `rgba(${r},${g},${b},${alpha})`;
  }
  if (h.length === 7) {
    const r = parseInt(h.slice(1, 3), 16);
    const g = parseInt(h.slice(3, 5), 16);
    const b = parseInt(h.slice(5, 7), 16);
    if ([r, g, b].some((n) => Number.isNaN(n))) {
      return `rgba(100, 116, 139, ${alpha})`;
    }
    return `rgba(${r},${g},${b},${alpha})`;
  }
  return `rgba(100, 116, 139, ${alpha})`;
}

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);

  /** `true` = classe `dark` sur `<html>`. */
  readonly isDark = signal(false);

  constructor() {
    const stored = readStoredDark();
    const prefersDark =
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-color-scheme: dark)')?.matches;
    this.isDark.set(stored ?? Boolean(prefersDark));
    this.applyClass();
  }

  toggle(): void {
    this.isDark.update((v) => !v);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, this.isDark() ? 'dark' : 'light');
    }
    this.applyClass();
  }

  private applyClass(): void {
    this.doc.documentElement.classList.toggle('dark', this.isDark());
  }
}
