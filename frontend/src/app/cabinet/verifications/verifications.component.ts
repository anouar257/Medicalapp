import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PractitionerService } from '../../services/practitioner.service';
import { PractitionerProfileDTO, VerificationStatus } from '../../models/practitioner.model';

/**
 * Vérification du compte praticien (cf. cahier : VÉRIFICATION DU COMPTE).
 *
 * <ul>
 *   <li>Vérifier par identité (passeport / carte d'identité) ;</li>
 *   <li>Vérifier par un document du ministère chargé de la santé (droit d'exercer).</li>
 * </ul>
 *
 * <p>Le praticien fournit une URL de document (upload externe à venir) puis le statut
 * passe à <em>EN_ATTENTE</em>. La validation finale est manuelle (admin / opérateur).
 */
@Component({
  selector: 'app-verifications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './verifications.component.html',
  styleUrls: ['./verifications.component.scss'],
})
export class VerificationsComponent implements OnInit {
  profile: PractitionerProfileDTO | null = null;
  docIdentiteUrl = '';
  docDroitUrl = '';
  savingIdentite = false;
  savingDroit = false;
  errorMessage = '';
  successMessage = '';

  private readonly destroyRef = inject(DestroyRef);

  constructor(private practitionerService: PractitionerService) {}

  ngOnInit() {
    this.practitionerService
      .me()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (p) => (this.profile = p),
        error: (e) => (this.errorMessage = e.error?.error || 'Profil introuvable'),
      });
  }

  submitIdentite() {
    if (!this.profile) return;
    this.savingIdentite = true;
    this.practitionerService
      .setVerification(this.profile.id, 'identite', 'EN_ATTENTE', this.docIdentiteUrl)
      .subscribe({
        next: (p) => {
          this.profile = p;
          this.savingIdentite = false;
          this.successMessage = "Document soumis. Vérification en cours.";
          setTimeout(() => (this.successMessage = ''), 3000);
        },
        error: (e) => {
          this.savingIdentite = false;
          this.errorMessage = e.error?.error || 'Erreur';
        },
      });
  }

  submitDroit() {
    if (!this.profile) return;
    this.savingDroit = true;
    this.practitionerService
      .setVerification(this.profile.id, 'droit-exercer', 'EN_ATTENTE', this.docDroitUrl)
      .subscribe({
        next: (p) => {
          this.profile = p;
          this.savingDroit = false;
          this.successMessage = "Document soumis. Vérification en cours.";
          setTimeout(() => (this.successMessage = ''), 3000);
        },
        error: (e) => {
          this.savingDroit = false;
          this.errorMessage = e.error?.error || 'Erreur';
        },
      });
  }

  statusLabel(s: VerificationStatus): string {
    switch (s) {
      case 'NON_FOURNI': return 'Non fourni';
      case 'EN_ATTENTE': return 'En attente';
      case 'VERIFIE': return 'Vérifié';
      case 'REFUSE': return 'Refusé';
    }
  }
}
