import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthProService } from '../../services/auth-pro.service';
import { PractitionerService } from '../../services/practitioner.service';
import {
  RegisterPractitionerRequest,
  SpecialtyDTO,
  StatutPraticien,
  Titre,
} from '../../models/practitioner.model';
import { AppNavbarComponent } from '../../shared/app-navbar.component';
import { PreferencesService } from '../../services/preferences.service';

/**
 * Inscription self-service d'un praticien (ou remplaçant).
 *
 * <p>Ce parcours implémente précisément la section <em>« INSCRIPTION DU PRATICIEN OU UN REMPLAÇANT »</em>
 * du cahier des charges :
 * <ol>
 *   <li>vérification d'existence (email / téléphone) ;</li>
 *   <li>identité du praticien (civilité, titre, sexe, date de naissance, statut) ;</li>
 *   <li>spécialités (au moins une) ;</li>
 *   <li>mot de passe + acceptation CGU.</li>
 * </ol>
 */
@Component({
  selector: 'app-register-practitioner',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AppNavbarComponent],
  templateUrl: './register-practitioner.component.html',
  styleUrls: ['./register-practitioner.component.scss'],
})
export class RegisterPractitionerComponent implements OnInit {
  step = 1;

  stepsConfig: { num: number; label: string }[] = [
    { num: 1, label: 'Email/Tél' },
    { num: 2, label: 'Identité' },
    { num: 3, label: 'Spécialités' },
    { num: 4, label: 'Validation' },
  ];

  data: RegisterPractitionerRequest = {
    titre: 'AUCUN' as Titre,
    sexe: 'HOMME',
    prenom: '',
    nom: '',
    dateNaissance: '',
    email: '',
    telephone: '',
    motDePasse: '',
    cguAcceptees: false,
    statut: 'TITULAIRE' as StatutPraticien,
    specialiteIds: [],
  };

  confirmPassword = '';
  showPassword = false;
  showConfirmPassword = false;
  loading = false;
  loadingSpecialties = false;
  errorMessage = '';
  successMessage = '';

  specialties: SpecialtyDTO[] = [];

  constructor(
    private authPro: AuthProService,
    private practitionerService: PractitionerService,
    private router: Router,
    public prefs: PreferencesService
  ) {}

  ngOnInit() {
    this.loadingSpecialties = true;
    this.practitionerService.listSpecialties().subscribe({
      next: (s) => {
        this.specialties = s;
        this.loadingSpecialties = false;
      },
      error: () => (this.loadingSpecialties = false),
    });
  }

  isSelected(id: number): boolean {
    return !!this.data.specialiteIds?.includes(id);
  }

  toggleSpecialty(id: number) {
    const arr = this.data.specialiteIds ?? [];
    if (arr.includes(id)) {
      this.data.specialiteIds = arr.filter((x) => x !== id);
    } else {
      this.data.specialiteIds = [...arr, id];
    }
  }

  checkExistence() {
    this.errorMessage = '';
    if (!this.data.email || !this.data.telephone) {
      this.errorMessage = 'Email et téléphone obligatoires';
      return;
    }
    this.loading = true;
    this.authPro.checkExistence(this.data.email, this.data.telephone).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.exists) {
          this.errorMessage = 'Un compte avec cet email ou ce téléphone existe déjà';
        } else {
          this.step = 2;
        }
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Erreur de vérification';
      },
    });
  }

  goToStep3() {
    this.errorMessage = '';
    if (!this.data.prenom || !this.data.nom || !this.data.sexe || !this.data.dateNaissance) {
      this.errorMessage = 'Veuillez compléter tous les champs obligatoires';
      return;
    }
    this.step = 3;
  }

  goToStep4() {
    this.errorMessage = '';
    if (!this.data.specialiteIds || this.data.specialiteIds.length === 0) {
      this.errorMessage = 'Veuillez sélectionner au moins une spécialité';
      return;
    }
    this.step = 4;
  }

  onSubmit() {
    this.errorMessage = '';
    if (this.data.motDePasse !== this.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas';
      return;
    }
    if (this.data.motDePasse.length < 8) {
      this.errorMessage = 'Le mot de passe doit contenir au moins 8 caractères';
      return;
    }
    this.loading = true;
    this.authPro.registerPractitioner(this.data).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Compte praticien créé. Vérifiez votre email/SMS.';
        setTimeout(() => {
          this.router.navigate(['/auth/verify-otp-pro'], {
            queryParams: { email: this.data.email, telephone: this.data.telephone },
          });
        }, 1500);
      },
      error: (e) => {
        this.loading = false;
        this.errorMessage = e.error?.error || "Erreur lors de l'inscription";
      },
    });
  }
}
