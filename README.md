# 🏥 MediAgenda - Plateforme de Gestion Médicale Moderne

MediAgenda est une solution complète de gestion de cabinet médical et de prise de rendez-vous en ligne, conçue avec une architecture microservices robuste et une interface utilisateur réactive et haute performance.

## 🛠️ Stack Technique

- **Backend :** Java 21, Spring Boot 3.4.2, Spring Security (JWT), Hibernate/JPA, OpenFeign.
- **Frontend :** Angular 18+, Tailwind CSS, RxJS, Signals (State Management).
- **Base de données :** PostgreSQL.
- **Gestion des fichiers :** Stockage local persistant pour les documents et photos médicales.

## 📋 Prérequis

Avant de lancer le projet, assurez-vous d'avoir installé :
- **Java JDK 21** (Indispensable pour le support des Virtual Threads et records).
- **Node.js v20+** et **npm**.
- **PostgreSQL 15+** en cours d'exécution.
- **Maven 3.9+**.

## 💾 Configuration de la Base de Données

Le projet utilise **4 bases de données distinctes** pour garantir l'isolation des données par service. Vous devez créer ces bases dans PostgreSQL avant le lancement :

1.  `agenda_medical` (Service Agenda :8081)
2.  `patient_medical` (Service Patient :8082)
3.  `practitioner_medical` (Service Praticien :8083)
4.  `messaging_medical` (Service Messagerie :8084)

**Important :** Les tables sont générées **automatiquement** par Hibernate lors du premier lancement de chaque service (`ddl-auto: update`).

### Paramètres de connexion (par défaut) :
- **Hôte :** `localhost:5432`
- **Utilisateur :** `postgres`
- **Mot de passe :** `odoo` *(à adapter dans les fichiers application.yml si besoin)*


## ⚡ Installation et Lancement

### 1. Démarrage du Backend (Ordre recommandé)

Chaque service doit être lancé dans son dossier respectif dans `backend/` :

```bash
# Pour chaque dossier (agenda-service, patient-service, practitioner-service, messaging-service) :
mvn clean install
mvn spring-boot:run
```

**Services et Ports :**
- `agenda-service` : **8081** (Cœur du système)
- `patient-service` : **8082** (Gestion des patients & comptes)
- `practitioner-service` : **8083** (Profils praticiens & authentification pro)
- `messaging-service` : **8084** (Messagerie sécurisée inter-services)

### 2. Démarrage du Frontend

```bash
cd frontend
npm install
ng serve
```
L'application sera accessible sur `http://localhost:4200`.

## ✨ Fonctionnalités Clés

- **Agenda Dynamique :** Gestion des rendez-vous en temps réel avec indicateurs visuels.
- **Messagerie Sécurisée :** Échange de documents et messages avec résolution automatique des noms via communication inter-services.
- **Multi-Rôles :** Espaces dédiés pour les Praticiens, les Assistants et les Patients.
- **Design Premium :** Support complet du **Mode Sombre**, accessibilité avancée et animations fluides.
- **Zéro Rechargement (No F5) :** Gestion d'état moderne via **Angular Signals** pour une réactivité instantanée.

---

