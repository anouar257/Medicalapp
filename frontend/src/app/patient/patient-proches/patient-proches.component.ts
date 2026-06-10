import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProcheService } from '../../services/proche.service';
import { AuthService } from '../../services/auth.service';
import { Proche } from '../../models/proche.model';
import { formatHttpError } from '../../utils/http-error-message';

@Component({
  selector: 'app-patient-proches',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patient-proches.component.html',
  styleUrls: ['./patient-proches.component.scss'],
})
export class PatientProchesComponent implements OnInit {
  proches: Proche[] = [];
  showForm = false;
  editingId: number | null = null;
  loading = false;
  loadingList = true;
  errorMessage = '';
  successMessage = '';

  formData: Proche = this.emptyProche();

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private procheService: ProcheService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.loadProches();
  }

  loadProches() {
    this.loadingList = true;
    this.procheService
      .getMyProches()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.proches = data;
          this.loadingList = false;
        },
        error: (e) => {
          this.loadingList = false;
          const fallback =
            e?.status === 404
              ? 'Impossible de charger vos proches. Si vous avez vidé la base de données, déconnectez-vous et reconnectez-vous.'
              : "Service indisponible. Vérifiez que le microservice patient-service est démarré.";
          this.errorMessage = formatHttpError(e, fallback);
        },
      });
  }

  onSubmit() {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const submissionData = { ...this.formData };
    const optionalFields: (keyof Proche)[] = [
      'civilite',
      'paysNaissance',
      'villeNaissance',
      'telephoneMobile',
      'email',
      'adresse',
      'codePostal',
      'ville',
      'ancienNomFamille',
      'telephoneFixe',
      'assurance',
      'remarque',
      'provenance',
      'profession',
      'medecinTraitant'
    ];
    optionalFields.forEach(field => {
      if (submissionData[field] === '') {
        (submissionData as any)[field] = null;
      }
    });

    const action$ = this.editingId
      ? this.procheService.updateProche(this.editingId, submissionData)
      : this.procheService.createProche(submissionData);

    action$.subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = this.editingId ? 'Proche modifié' : 'Proche ajouté';
        this.loadProches();
        setTimeout(() => this.resetForm(), 1500);
      },
      error: (e) => {
        this.loading = false;
        this.errorMessage = formatHttpError(e, 'Erreur');
      },
    });
  }

  editProche(proche: Proche) {
    const editData = { ...proche };
    const optionalFields: (keyof Proche)[] = [
      'civilite',
      'paysNaissance',
      'villeNaissance',
      'telephoneMobile',
      'email',
      'adresse',
      'codePostal',
      'ville',
      'ancienNomFamille',
      'telephoneFixe',
      'assurance',
      'remarque',
      'provenance',
      'profession',
      'medecinTraitant'
    ];
    optionalFields.forEach(field => {
      if (editData[field] === null || editData[field] === undefined) {
        (editData as any)[field] = '';
      }
    });

    this.formData = editData;
    this.editingId = proche.id || null;
    this.showForm = true;
  }

  deleteProche(id: number) {
    if (!confirm('Supprimer ce proche ?')) return;
    this.procheService.deleteProche(id).subscribe({
      next: () => this.loadProches(),
      error: (e) => {
        this.errorMessage = formatHttpError(e, 'Erreur');
      },
    });
  }

  toggleForm() {
    if (this.showForm) {
      this.resetForm();
    } else {
      this.showForm = true;
    }
  }

  resetForm() {
    this.formData = this.emptyProche();
    this.editingId = null;
    this.showForm = false;
    this.errorMessage = '';
    this.successMessage = '';
  }

  private emptyProche(): Proche {
    return {
      civilite: '' as any,
      sexe: '' as any,
      prenom: '', nom: '',
      nomFamilleChange: false, ancienNomFamille: '',
      dateNaissance: '', paysNaissance: '', villeNaissance: '',
      telephoneMobile: '', telephoneFixe: '', email: '',
      adresse: '', codePostal: '', ville: '',
      assurance: '', remarque: '', provenance: '',
      profession: '', medecinTraitant: '',
      envoiSmsActive: true, envoiEmailActive: true,
      pieceIdentiteValidee: false, identiteDouteuse: false
    };
  }
}
