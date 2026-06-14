import { TranslateLoader } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function deepMerge(target: Record<string, unknown>, source: Record<string, unknown>): Record<string, unknown> {
  for (const [key, value] of Object.entries(source)) {
    if (isPlainObject(value)) {
      const current = isPlainObject(target[key]) ? (target[key] as Record<string, unknown>) : {};
      target[key] = deepMerge(current, value);
    } else {
      target[key] = value;
    }
  }

  return target;
}

function setNested(target: Record<string, unknown>, dottedKey: string, value: unknown): void {
  const parts = dottedKey.split('.').filter(Boolean);
  if (parts.length === 0) {
    return;
  }

  let cursor = target;
  for (let index = 0; index < parts.length - 1; index += 1) {
    const part = parts[index];
    if (!isPlainObject(cursor[part])) {
      cursor[part] = {};
    }
    cursor = cursor[part] as Record<string, unknown>;
  }

  const last = parts[parts.length - 1];
  if (isPlainObject(value)) {
    const current = isPlainObject(cursor[last]) ? (cursor[last] as Record<string, unknown>) : {};
    cursor[last] = deepMerge(current, value);
  } else {
    cursor[last] = value;
  }
}

function normalizeTranslationTree(node: unknown, prefix = '', out: Record<string, unknown> = {}): Record<string, unknown> {
  if (!isPlainObject(node)) {
    return out;
  }

  for (const [key, value] of Object.entries(node)) {
    const dottedKey = prefix ? `${prefix}.${key}` : key;
    if (isPlainObject(value)) {
      normalizeTranslationTree(value, dottedKey, out);
    } else {
      setNested(out, dottedKey, value);
    }
  }

  return out;
}

export class MultiTranslateHttpLoader implements TranslateLoader {
  constructor(
    private http: HttpClient,
    private prefix: string = './assets/i18n/',
    private files: string[] = [
      'common', 'auth', 'dashboard', 'patient', 'practitioner', 'landing',
      'assistant', 'admin', 'appointment', 'payment', 'messaging', 'errors', 'overrides'
    ]
  ) {}

  public getTranslation(lang: string): Observable<any> {
    const requests = this.files.map(file => {
      return this.http.get(`${this.prefix}${lang}/${file}.json`).pipe(
        map(res => ({ file, data: res })),
        catchError((err) => {
          console.warn(`Could not find translation file: ${this.prefix}${lang}/${file}.json`, err);
          return of({ file, data: {} });
        })
      );
    });

    return forkJoin(requests).pipe(
      map(responses => {
        const merged: any = {};
        responses.forEach(({ file, data }) => {
          deepMerge(merged, normalizeTranslationTree(data));
        });
        return merged;
      })
    );
  }
}
