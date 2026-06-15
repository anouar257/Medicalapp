import { Component, HostListener, OnInit, OnDestroy, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  Subject,
  Subscription,
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  forkJoin,
  map,
  of,
  switchMap,
} from 'rxjs';
import { ThemeService } from '../../services/theme.service';
import { PractitionerService } from '../../services/practitioner.service';
import { AgendaService } from '../../services/agenda.service';
import { PreferencesService, AppLanguage, ZoomLevel } from '../../services/preferences.service';
import { AppNavbarComponent } from '../../shared/app-navbar.component';
import { AiAssistantChatComponent } from '../../shared/ai-assistant-chat/ai-assistant-chat.component';
import type { SpecialtyDTO } from '../../models/practitioner.model';
import {
  type CombinedPractitionerOption,
  filterCombinedOptions,
  mergePractitionerSearchResults,
} from '../../patient/patient-search-merge';
import { resolveDoctorPhotoUrl, getDynamicAvatar } from '../../utils/media-url';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink, AppNavbarComponent, AiAssistantChatComponent],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.scss'],
})
export class LandingPageComponent implements OnInit, OnDestroy {
  readonly theme = inject(ThemeService);
  readonly prefs = inject(PreferencesService);
  private readonly practitionerService = inject(PractitionerService);
  private readonly agendaService = inject(AgendaService);
  private eRef = inject(ElementRef);

  readonly currentYear = new Date().getFullYear();

  specialtyCatalog: SpecialtyDTO[] = [];
  specialtyFilterText = '';
  selectedSpecialtyLabel = '';
  isSpecialtyDropdownOpen = false;
  currentCarouselIndex = 0;
  private carouselInterval: any;

  practitionerName = '';
  city = '';

  isCityDropdownOpen = false;
  cityFilterText = '';
  cities: string[] = ['Casablanca', 'Rabat', 'Marrakech', 'Fès', 'Tanger', 'Agadir', 'Meknès', 'Oujda', 'Kénitra', 'Tétouan'];
  filteredCities: string[] = [...this.cities];

  private readonly searchSubject = new Subject<{ name: string; city: string; specialty: string }>();
  isSearching = false;
  showSearchResults = false;
  results: CombinedPractitionerOption[] = [];
  private searchSub?: Subscription;

  resolvePhoto(url: string | undefined | null, name?: string): string {
    return resolveDoctorPhotoUrl(url, name);
  }

  onImageError(event: Event, name: string) {
    const img = event.target as HTMLImageElement;
    img.src = getDynamicAvatar(name);
  }

  starsFilled(rating: number | null | undefined): number {
    if (rating == null) return 0;
    return Math.round(rating);
  }

  profileStars(): number[] {
    return [1, 2, 3, 4, 5];
  }

  translate(key: string): string {
    return this.prefs.translate(key);
  }

  ngOnInit(): void {
    this.practitionerService.listSpecialties().subscribe({
      next: (list) => (this.specialtyCatalog = list ?? []),
      error: () => (this.specialtyCatalog = []),
    });

    this.practitionerService.listPublicCities().subscribe({
      next: (list) => {
        if (list && list.length > 0) {
          this.cities = list;
          this.filteredCities = [...this.cities];
        }
      },
      error: () => {},
    });

    this.searchSub = this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => a.name === b.name && a.city === b.city && a.specialty === b.specialty),
        switchMap((q) => {
          this.isSearching = true;
          return forkJoin({
            pros: this.practitionerService
              .searchPublic({ name: q.name, city: q.city, specialty: q.specialty })
              .pipe(catchError(() => of([]))),
            doctors: this.agendaService.listDoctors().pipe(catchError(() => of([]))),
          }).pipe(
            map(({ pros, doctors }) => {
              const merged = mergePractitionerSearchResults(pros, doctors);
              return filterCombinedOptions(merged, q.name, q.city, q.specialty);
            }),
            finalize(() => (this.isSearching = false)),
          );
        }),
      )
      .subscribe({
        next: (res) => {
          this.results = res;
          this.isSearching = false;
        },
        error: () => {
          this.results = [];
          this.isSearching = false;
        },
      });

    this.startCarouselAutoPlay();
  }

  ngOnDestroy(): void {
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
    if (this.carouselInterval) {
      clearInterval(this.carouselInterval);
    }
  }

  private startCarouselAutoPlay(): void {
    this.carouselInterval = setInterval(() => {
      this.currentCarouselIndex = (this.currentCarouselIndex + 1) % 3;
    }, 5000);
  }

  setCarouselIndex(index: number): void {
    this.currentCarouselIndex = index;
    clearInterval(this.carouselInterval);
    this.startCarouselAutoPlay();
  }

  bookingLoginQuery(r: CombinedPractitionerOption): Record<string, string> {
    const q: Record<string, string> = { returnUrl: '/patient/prendre-rendez-vous' };
    if (r.practitionerId != null) q['practitionerId'] = String(r.practitionerId);
    if (r.agendaDoctorId != null) q['agendaDoctorId'] = String(r.agendaDoctorId);
    return q;
  }

  changeLang(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.prefs.setLanguage(target.value as AppLanguage);
  }

  setZoom(level: ZoomLevel) {
    this.prefs.setZoom(level);
  }

  selectSpecialty(s: SpecialtyDTO | null): void {
    this.showSearchResults = true;
    if (!s) {
      this.specialtyFilterText = '';
      this.selectedSpecialtyLabel = '';
    } else {
      this.specialtyFilterText = s.code;
      this.selectedSpecialtyLabel = s.libelle;
    }
    this.isSpecialtyDropdownOpen = false;
    this.triggerSearch();
  }

  onSpecialtySelected(specialtyCode: string): void {
    const spec = this.specialtyCatalog.find(x => x.code === specialtyCode);
    this.specialtyFilterText = specialtyCode;
    this.selectedSpecialtyLabel = spec ? spec.libelle : specialtyCode;
    this.showSearchResults = true;
    this.triggerSearch();

    // Scroll to search or results section
    const searchCard = document.querySelector('.relative.w-full.max-w-5xl.mt-12');
    if (searchCard) {
      searchCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  openSearchPanel(): void {
    this.showSearchResults = true;
    this.triggerSearch();
  }

  triggerSearch(): void {
    const q = { name: this.practitionerName, city: this.city, specialty: this.specialtyFilterText };
    if (q.name.trim() || q.city.trim() || q.specialty.trim()) {
      this.showSearchResults = true;
    }
    this.searchSubject.next(q);
  }

  onPractitionerNameInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.practitionerName = input.value;
    this.showSearchResults = true;
    this.triggerSearch();
  }

  openCityDropdown(): void {
    this.isCityDropdownOpen = !this.isCityDropdownOpen;
    this.isSpecialtyDropdownOpen = false;
  }

  onCityInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cityFilterText = input.value;
    if (this.cityFilterText.trim() === '') {
      this.filteredCities = [...this.cities];
    } else {
      const q = this.cityFilterText.toLowerCase();
      this.filteredCities = this.cities.filter((c) => c.toLowerCase().includes(q));
    }
  }

  selectCity(c: string | null): void {
    this.city = c || '';
    this.cityFilterText = '';
    this.filteredCities = [...this.cities];
    this.isCityDropdownOpen = false;
    this.showSearchResults = true;
    this.triggerSearch();
  }

  @HostListener('document:click', ['$event'])
  clickout(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.isSpecialtyDropdownOpen = false;
      this.isCityDropdownOpen = false;
    }
  }

  // ── Helpers Carousel Stack ───────────────────────────────────────────

  getStackTransform(index: number): string {
    const diff = index - this.currentCarouselIndex;
    const absDiff = Math.abs(diff);

    if (diff === 0) return 'translateX(0) scale(1) translateZ(0)';
    
    // Positionnement latéral
    const x = diff * 320; // Décalage horizontal
    const scale = 1 - (absDiff * 0.15); // Réduction de taille
    const rotate = diff * 10; // Inclinaison légère

    return `translateX(${x}px) scale(${scale}) rotateY(${rotate}deg)`;
  }

  getStackZIndex(index: number): number {
    return 10 - Math.abs(index - this.currentCarouselIndex);
  }

  getStackOpacity(index: number): number {
    const absDiff = Math.abs(index - this.currentCarouselIndex);
    if (absDiff === 0) return 1;
    if (absDiff === 1) return 0.6;
    return 0;
  }
}
