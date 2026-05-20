export type Civilite = 'M' | 'MME' | 'MLLE';
export type Sexe = 'HOMME' | 'FEMME';

export interface Proche {
  id?: number;
  civilite: Civilite;
  sexe: Sexe;
  prenom: string;
  nom: string;
  nomFamilleChange: boolean;
  ancienNomFamille?: string;
  dateNaissance: string;
  paysNaissance: string;
  villeNaissance: string;
  telephoneMobile: string;
  telephoneFixe?: string;
  email: string;
  adresse: string;
  codePostal: string;
  ville: string;
  assurance?: string;
  remarque?: string;
  provenance?: string;
  profession?: string;
  medecinTraitant?: string;
  envoiSmsActive: boolean;
  envoiEmailActive: boolean;
  pieceIdentiteValidee: boolean;
  identiteDouteuse: boolean;
  dateCreation?: string;
}
