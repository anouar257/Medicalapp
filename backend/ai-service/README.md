# MedConnect AI Service

Ce microservice en Python FastAPI fournit des recommandations d'orientation médicale basées sur les symptômes des patients.

## Fonctionnalités
- **Recommandations d'orientation** : Recommande entre 1 et 3 spécialités adaptées à partir de descriptions en langage naturel.
- **Support multilingue** : Réponses en français (`fr`), anglais (`en`), et arabe (`ar`).
- **Moteur de règles locales** : Utilise un dictionnaire de mots-clés local en cas d'absence de la clé d'API Gemini, de timeout ou d'erreur réseau.
- **Détection des urgences** : Identifie les symptômes graves pour orienter les patients vers des soins d'urgence.

## Endpoints
- `GET /health` : Retourne l'état de fonctionnement du service.
- `POST /api/ai/orientation` : Traite la description des symptômes.

### Exemple de requête
```json
{
  "message": "J’ai mal à la gorge et de la fièvre",
  "language": "fr"
}
```

### Exemple de réponse
```json
{
  "message": "Je ne peux pas poser de diagnostic, mais je peux vous orienter.",
  "specialties": ["Médecin généraliste", "ORL"],
  "urgency": "normal",
  "warning": "Cet assistant ne remplace pas une consultation médicale. En cas d'urgence, contactez les urgences."
}
```
