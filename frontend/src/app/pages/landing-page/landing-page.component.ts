import { Component, HostListener, OnInit, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  forkJoin,
  map,
  of,
  shareReplay,
  switchMap,
  type Observable,
} from 'rxjs';
import { ThemeService } from '../../services/theme.service';
import { PractitionerService } from '../../services/practitioner.service';
import { AgendaService } from '../../services/agenda.service';
import { PreferencesService, AppLanguage, ZoomLevel } from '../../services/preferences.service';
import { APP_DICTIONARY } from '../../i18n/app-dictionary';
import type { SpecialtyDTO } from '../../models/practitioner.model';
import {
  type CombinedPractitionerOption,
  filterCombinedOptions,
  mergePractitionerSearchResults,
} from '../../patient/patient-search-merge';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.scss'],
})
export class LandingPageComponent implements OnInit {
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

  private readonly searchSubject = new Subject<{ name: string; city: string; specialty: string }>();
  isSearching = false;
  showSearchResults = false;

  readonly combinedResults$: Observable<CombinedPractitionerOption[]> = this.searchSubject.pipe(
    debounceTime(300),
    distinctUntilChanged(
      (a, b) => a.name === b.name && a.city === b.city && a.specialty === b.specialty,
    ),
    switchMap((q) => {
      this.isSearching = true;
      return forkJoin({
        pros: this.practitionerService
          .searchPublic({ name: '', city: '', specialty: '' })
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
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  /** Textes propres à la page d’accueil ; fusion avec le dictionnaire global dans translate(). */
  private readonly dictionary: Record<string, Record<AppLanguage, string>> = {
    'Espace Patient': { fr: 'Espace Patient', en: 'Patient Portal', ar: 'بوابة المريض' },
    'Espace Pro': { fr: 'Espace Pro', en: 'Pro Portal', ar: 'بوابة الطبيب' },
    'Trouvez votre médecin': { fr: 'Trouvez votre médecin', en: 'Find your doctor', ar: 'ابحث عن طبيبك' },
    'Prenez rendez-vous en ligne.': { fr: 'et prenez rendez-vous en ligne.', en: 'and book appointments online.', ar: 'واحجز موعدك عبر الإنترنت.' },
    'Simple, rapide et sécurisé. Votre santé à portée de clic.': {
      fr: 'Simple, rapide et sécurisé. Votre santé à portée de clic.',
      en: 'Simple, fast, and secure. Your health just a click away.',
      ar: 'بسيط وسريع وآمن. صحتك على بعد نقرة واحدة.',
    },
    'Nom du praticien...': { fr: 'Nom du praticien...', en: 'Doctor name...', ar: 'اسم الطبيب...' },
    'Spécialité': { fr: 'Spécialité', en: 'Specialty', ar: 'التخصص' },
    'Où ? (Ville)': { fr: 'Où ? (Ville, Code postal)', en: 'Where? (City, Zip)', ar: 'أين؟ (المدينة، الرمز البريدي)' },
    Rechercher: { fr: 'Rechercher', en: 'Search', ar: 'بحث' },
    'Actualités & Prévention': { fr: 'Actualités & Prévention', en: 'News & Prevention', ar: 'أخبار ووقاية' },
    PRÉVENTION: { fr: 'PRÉVENTION', en: 'PREVENTION', ar: 'وقاية' },
    'Campagne de vaccination': { fr: 'Campagne de vaccination grippe', en: 'Flu Vaccination Campaign', ar: 'حملة التطعيم ضد الإنفلونزا' },
    'Prenez rendez-vous facilement près de chez vous pour vous protéger.': {
      fr: 'Prenez rendez-vous facilement près de chez vous pour vous protéger.',
      en: 'Easily book an appointment near you to protect yourself.',
      ar: 'احجز موعدًا بسهولة بالقرب منك لحماية نفسك.',
    },
    NOUVEAU: { fr: 'NOUVEAU', en: 'NEW', ar: 'جديد' },
    'Centre Médical Lumière': { fr: 'Centre Médical Lumière', en: 'Lumiere Medical Center', ar: 'مركز النور الطبي' },
    "Ouverture d'un nouveau centre de radiologie. Prenez RDV en ligne.": {
      fr: "Ouverture d'un nouveau centre de radiologie. Prenez RDV en ligne.",
      en: 'Opening of a new radiology center. Book online.',
      ar: 'افتتاح مركز أشعة جديد. احجز موعدك عبر الإنترنت.',
    },
    INFO: { fr: 'INFO', en: 'INFO', ar: 'معلومة' },
    "Mise à jour de l'application": { fr: "Mise à jour de l'application", en: 'Application Update', ar: 'تحديث التطبيق' },
    'Découvrez les nouvelles fonctionnalités de votre espace patient.': {
      fr: 'Découvrez les nouvelles fonctionnalités de votre espace patient.',
      en: 'Discover new features in your patient portal.',
      ar: 'اكتشف الميزات الجديدة في بوابة المريض الخاصة بك.',
    },
    'Comment utiliser MedConnect ?': { fr: 'Comment utiliser MedConnect ?', en: 'How to use MedConnect?', ar: 'كيف تستخدم MedConnect؟' },
    '1. Recherchez': { fr: '1. Recherchez', en: '1. Search', ar: '1. ابحث' },
    'Trouvez un praticien par nom ou spécialité près de chez vous en quelques clics.': {
      fr: 'Trouvez un praticien par nom ou spécialité près de chez vous en quelques clics.',
      en: 'Find a practitioner by name or specialty near you in a few clicks.',
      ar: 'ابحث عن ممارس بالاسم أو التخصص بالقرب منك ببضع نقرات.',
    },
    '2. Choisissez': { fr: '2. Choisissez un créneau', en: '2. Choose a slot', ar: '2. اختر موعدًا' },
    'Consultez les disponibilités en temps réel et choisissez le créneau qui vous convient.': {
      fr: 'Consultez les disponibilités en temps réel et choisissez le créneau qui vous convient.',
      en: 'View real-time availability and pick the slot that fits you.',
      ar: 'تحقق من التوافر في الوقت الفعلي واختر الوقت الذي يناسبك.',
    },
    '3. Réservez': { fr: '3. Prenez rendez-vous', en: '3. Book Appointment', ar: '3. احجز موعدًا' },
    'Confirmez votre RDV en toute sécurité et recevez vos rappels automatiques.': {
      fr: 'Confirmez votre RDV en toute sécurité et recevez vos rappels automatiques.',
      en: 'Confirm your appointment securely and get automatic reminders.',
      ar: 'قم بتأكيد موعدك بأمان واحصل على تذكيرات تلقائية.',
    },
    'La plateforme de prise de rendez-vous médicaux de nouvelle génération.': {
      fr: 'La plateforme de prise de rendez-vous médicaux de nouvelle génération.',
      en: 'The next-generation medical appointment booking platform.',
      ar: 'منصة حجز المواعيد الطبية من الجيل القادم.',
    },
    'À propos': { fr: 'À propos', en: 'About', ar: 'حول' },
    'Qui sommes-nous ?': { fr: 'Qui sommes-nous ?', en: 'Who are we?', ar: 'من نحن؟' },
    Recrutement: { fr: 'Recrutement', en: 'Careers', ar: 'توظيف' },
    'On embauche': { fr: 'On embauche', en: 'Hiring', ar: 'نوظف' },
    Presse: { fr: 'Presse', en: 'Press', ar: 'صحافة' },
    'Légal & Aide': { fr: 'Légal & Aide', en: 'Legal & Help', ar: 'قانوني ومساعدة' },
    'Aide et Contact': { fr: 'Aide et Contact', en: 'Help & Contact', ar: 'مساعدة واتصال' },
    "Note d'engagement": { fr: "Note d'engagement", en: 'Commitment Note', ar: 'ملاحظة الالتزام' },
    'Confidentialité & RGPD': { fr: 'Confidentialité & RGPD', en: 'Privacy & GDPR', ar: 'الخصوصية' },
    'Conditions Générales (CGU)': { fr: 'Conditions Générales (CGU)', en: 'Terms of Service', ar: 'الشروط والأحكام' },
    Praticiens: { fr: 'Praticiens', en: 'Practitioners', ar: 'الممارسون' },
    'Vous êtes professionnel de santé ? Équipez votre cabinet avec notre solution.': {
      fr: 'Vous êtes professionnel de santé ? Équipez votre cabinet avec notre solution.',
      en: 'Are you a healthcare professional? Equip your practice with our solution.',
      ar: 'هل أنت أخصائي رعاية صحية؟ جهز عيادتك بحلنا.',
    },
    'Inscrire mon cabinet': { fr: 'Inscrire mon cabinet', en: 'Register my practice', ar: 'تسجيل عيادتي' },
    'Aucun praticien trouvé.': { fr: 'Aucun praticien trouvé.', en: 'No practitioners found.', ar: 'لم يتم العثور على أطباء.' },
    'Prendre RDV': { fr: 'Prendre RDV', en: 'Book appointment', ar: 'حجز موعد' },
    'Tous droits réservés.': { fr: 'Tous droits réservés.', en: 'All rights reserved.', ar: 'كل الحقوق محفوظة.' },
    'Vous êtes professionnel ?': { fr: 'Vous êtes professionnel de santé ?', en: 'Are you a healthcare professional?', ar: 'هل أنت أخصائي رعاية صحية؟' },
    'Réduisez vos appels': { 
      fr: 'Réduisez vos appels de 30% grâce à notre agenda intelligent.', 
      en: 'Reduce your calls by 30% with our smart agenda.', 
      ar: 'قلل مكالماتك بنسبة 30% من خلال أجندتنا الذكية.' 
    },
    'Découvrir la solution': { fr: 'Découvrir la solution', en: 'Discover the solution', ar: 'اكتشف الحل' },
  };

  ngOnInit(): void {
    this.practitionerService.listSpecialties().subscribe({
      next: (list) => (this.specialtyCatalog = list ?? []),
      error: () => (this.specialtyCatalog = []),
    });

    this.startCarouselAutoPlay();
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
      this.specialtyFilterText = s.libelle;
      this.selectedSpecialtyLabel = s.libelle;
    }
    this.isSpecialtyDropdownOpen = false;
    this.triggerSearch();
  }

  openSearchPanel(): void {
    this.showSearchResults = true;
    this.triggerSearch();
  }

  triggerSearch(): void {
    const q = {
      name: this.practitionerName,
      city: this.city,
      specialty: this.specialtyFilterText,
    };
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

  onCityInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.city = input.value;
    this.showSearchResults = true;
    this.triggerSearch();
  }

  @HostListener('document:click', ['$event'])
  clickout(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.isSpecialtyDropdownOpen = false;
    }
  }

  translate(key: string): string {
    const lang = this.prefs.language();
    const row = APP_DICTIONARY[key] ?? this.dictionary[key];
    return row?.[lang] ?? key;
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
