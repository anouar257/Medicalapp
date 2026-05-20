import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { filter, merge, of } from 'rxjs';
import { PreferencesService } from './services/preferences.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`,
  styles: [],
})
export class AppComponent {
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly prefs = inject(PreferencesService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    merge(
      of(null),
      this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)),
      toObservable(this.prefs.language),
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyDocumentTitle());
  }

  private applyDocumentTitle(): void {
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    const key = route.data['pageTitleKey'] as string | undefined;
    if (key) {
      this.title.setTitle(this.prefs.translate(key) + this.prefs.translate('docTitle.suffix'));
    } else {
      this.title.setTitle(this.prefs.translate('docTitle.default'));
    }
  }
}
