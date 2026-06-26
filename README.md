# 🏥 Mediconnect — Plateforme de Gestion Médicale Moderne

Mediconnect est une solution complète de gestion de cabinet médical et de prise de rendez-vous en ligne, conçue avec une architecture microservices robuste et une interface utilisateur réactive et haute performance.

## 🛠️ Stack Technique

| Couche | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4.2, Spring Cloud 2024.0.0 (Eureka, Gateway), Spring Security (JWT), Hibernate/JPA |
| **Message Broker** | Apache Kafka 3.7.0 (mode KRaft — sans Zookeeper) |
| **Intelligence Artificielle** | Python 3.11, FastAPI 0.110, Google Gemini API (avec fallback règles locales) |
| **Frontend** | Angular 19.2, Tailwind CSS 3.4, RxJS, Signals (State Management) |
| **Base de données** | PostgreSQL 15 |

## 📐 Architecture & Ports

| Service | Port | Rôle |
|---|---|---|
| `eureka-server` | **8761** | Serveur de découverte (annuaire des services) |
| `api-gateway` | **8090** | Passerelle API / Routage |
| `agenda-service` | **8081** | Gestion des rendez-vous (cœur du système) |
| `patient-service` | **8082** | Gestion des patients & comptes |
| `practitioner-service` | **8083** | Profils praticiens & authentification pro |
| `messaging-service` | **8084** | Messagerie sécurisée inter-services |
| `payment-service` | **8085** | Gestion des paiements & facturation |
| `ai-service` | **8001** | Orientation médicale IA (Python FastAPI) |
| `kafka` | **9092** | Broker d'événements (notifications RDV, facturation) |
| `postgres-db` | **5432** | Base de données relationnelle |
| `frontend` | **4200** | Interface utilisateur Angular |

## ✨ Fonctionnalités Clés

- **Architecture Microservices :** Découverte dynamique via Eureka Server et routage via API Gateway.
- **Agenda Dynamique :** Gestion des rendez-vous en temps réel avec indicateurs visuels.
- **Orientation IA :** Recommandation de spécialités médicales basée sur les symptômes (Gemini API + fallback local).
- **Messagerie Sécurisée :** Échange de documents et messages avec résolution automatique des noms.
- **Notifications en temps réel :** Événements Kafka pour notifications de RDV et facturation automatique.
- **Multi-Rôles :** Espaces dédiés pour les Praticiens, les Assistants et les Patients.
- **Design Premium :** Support complet du **Mode Sombre**, accessibilité avancée et animations fluides.
- **Zéro Rechargement :** Gestion d'état moderne via **Angular Signals** pour une réactivité instantanée.

---

# 🐳 Méthode 1 — Lancement avec Docker (Recommandé)

> C'est la méthode la plus simple. Il suffit d'installer **Docker Desktop** et **Git**. Pas besoin d'installer Java, Maven, Node.js, PostgreSQL ni Kafka.

## Prérequis Docker

| Logiciel | Téléchargement |
|---|---|
| **Git** | [git-scm.com/downloads](https://git-scm.com/downloads) |
| **Docker Desktop** (inclut Docker Compose) | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) |

> **Espace disque :** Prévoir ~10 Go pour le premier build (images Docker + compilation).
> **RAM :** 8 Go minimum recommandé.

## Étapes Docker

### Étape 1 — Cloner le projet

```bash
git clone https://github.com/anouar257/Medicalapp.git
cd Medicalapp
```

### Étape 2 — Créer le fichier `.env`

Le fichier `.env` contient les secrets JWT et la clé API Gemini. Il est **obligatoire** pour Docker Compose.

```bash
# Windows PowerShell :
Copy-Item .env.example .env

# Linux / Mac :
cp .env.example .env
```

Ouvrir le fichier `.env` créé et adapter les valeurs :

```properties
# Secrets JWT (les valeurs par défaut fonctionnent pour le développement)
JWT_SECRET=CHANGE_ME_PATIENT_JWT_SECRET_MIN_32_CHARS
JWT_PRO_SECRET=CHANGE_ME_PRO_JWT_SECRET_MIN_32_CHARS

# Clé API Google Gemini pour le service IA (orientation médicale)
GEMINI_API_KEY=VOTRE_CLE_ICI
```

**Pour obtenir une clé API Gemini :**
1. Aller sur [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Se connecter avec un compte Google
3. Cliquer sur **"Create API Key"**
4. Copier la clé générée et la coller dans le fichier `.env` à la place de `VOTRE_CLE_ICI`

> **Note :** La clé Gemini est **optionnelle**. Sans clé valide, le service IA fonctionnera quand même en utilisant un moteur de règles locales (résultats moins précis mais pas d'erreur).

### Étape 3 — Lancer le projet

```bash
docker compose up --build -d
```

> **Premier lancement :** Comptez **10 à 20 minutes** (téléchargement des images Docker + compilation Maven et Angular).
> **Lancements suivants :** 1 à 3 minutes (cache Docker).

### Étape 4 — Vérifier le déploiement

```bash
docker compose ps
```

Les **11 conteneurs** doivent être en état **"Up"** :
`postgres-db`, `eureka-server`, `api-gateway`, `agenda-service`, `patient-service`, `practitioner-service`, `messaging-service`, `payment-service`, `ai-service`, `kafka`, `frontend`.

### Étape 5 — Accéder à l'application

| URL | Description |
|---|---|
| **http://localhost:4200** | Application frontend |
| **http://localhost:8761** | Dashboard Eureka (services enregistrés) |
| **http://localhost:8001/health** | Health check du service IA |

### Commandes Docker utiles

```bash
# Voir les logs d'un service
docker compose logs -f agenda-service

# Voir les logs de tous les services
docker compose logs -f

# Arrêter tout
docker compose down

# Arrêter et supprimer toutes les données (volumes)
docker compose down -v

# Reconstruire un seul service
docker compose up -d --build patient-service

# Vérifier les événements Kafka
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic appointment-events \
  --from-beginning
```

### Résolution de problèmes Docker

| Problème | Cause | Solution |
|---|---|---|
| Port `5432` déjà utilisé | PostgreSQL local actif | Arrêter PostgreSQL local |
| Port `4200` déjà utilisé | `ng serve` déjà actif | Fermer le terminal Angular |
| Conteneur redémarre en boucle | Dépendance pas encore prête | Attendre ~1 min (restart automatique) |
| Build Maven échoue | Connexion internet instable | Vérifier le réseau et relancer `docker compose up --build -d` |

---

# 🖥️ Méthode 2 — Lancement Manuel (Sans Docker)

> Cette méthode lance chaque service individuellement. Elle nécessite l'installation de tous les outils de développement sur votre machine.

## Prérequis Manuels

Installer **tous** les logiciels suivants avant de commencer :

| Logiciel | Version | Téléchargement | Vérification |
|---|---|---|---|
| **Git** | 2.x+ | [git-scm.com/downloads](https://git-scm.com/downloads) | `git --version` |
| **Java JDK 21** | 21 | [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21) | `java -version` |
| **Apache Maven** | 3.9+ | [maven.apache.org/download](https://maven.apache.org/download.cgi) | `mvn -version` |
| **Node.js** | 20+ | [nodejs.org](https://nodejs.org/) | `node -v` |
| **Angular CLI** | 19.x | Installer via : `npm install -g @angular/cli` | `ng version` |
| **PostgreSQL** | 15+ | [postgresql.org/download](https://www.postgresql.org/download/) | `psql --version` |
| **Python** | 3.11+ | [python.org/downloads](https://www.python.org/downloads/) | `python --version` |

> **Important :** Java **21** est obligatoire (Virtual Threads, Records). Les versions 17 ou antérieures ne fonctionneront pas.

## Étapes Manuelles

### Étape 1 — Cloner le projet

```bash
git clone https://github.com/anouar257/Medicalapp.git
cd Medicalapp
```

### Étape 2 — Créer les 5 bases de données PostgreSQL

Ouvrir un terminal et se connecter à PostgreSQL :

```bash
psql -U postgres
```

> Le mot de passe attendu par le projet est `odoo`. Si votre mot de passe local est différent, vous pouvez le changer dans PostgreSQL ou passer la variable `SPRING_DATASOURCE_PASSWORD=votre_mot_de_passe` au lancement de chaque service Java.

Exécuter ces commandes SQL :

```sql
CREATE DATABASE agenda_medical;
CREATE DATABASE patient_medical;
CREATE DATABASE practitioner_medical;
CREATE DATABASE messaging_medical;
CREATE DATABASE payment_medical;
\q
```

> Les **tables** seront créées **automatiquement** par Hibernate (`ddl-auto: update`) au premier lancement de chaque service.

### Étape 3 — Définir les variables d'environnement

Les variables ci-dessous sont **obligatoires**. Sans elles, les services Java ne démarreront pas.

**Ces variables doivent être définies dans chaque terminal** avant de lancer un service.

**Windows PowerShell :**
```powershell
$env:JWT_SECRET = "CHANGE_ME_PATIENT_JWT_SECRET_MIN_32_CHARS"
$env:JWT_PRO_SECRET = "CHANGE_ME_PRO_JWT_SECRET_MIN_32_CHARS"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
```

**Linux / Mac (Bash) :**
```bash
export JWT_SECRET="CHANGE_ME_PATIENT_JWT_SECRET_MIN_32_CHARS"
export JWT_PRO_SECRET="CHANGE_ME_PRO_JWT_SECRET_MIN_32_CHARS"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
export EUREKA_SERVER_URL="http://localhost:8761/eureka/"
```

> **Pourquoi `KAFKA_BOOTSTRAP_SERVERS` et `EUREKA_SERVER_URL` ?** Par défaut, certains services pointent vers les hostnames Docker (`kafka:9092`, `eureka-server:8761`). Ces variables redirigent vers `localhost` pour le mode local.

### Étape 4 — Lancer Kafka (Optionnel)

Les services `agenda-service`, `messaging-service` et `payment-service` utilisent Kafka pour les événements (notifications de RDV, facturation automatique).

**Option A — Lancer Kafka via Docker (recommandé, même en mode manuel) :**
```bash
docker compose up -d kafka
```

**Option B — Sans Kafka :** Les services démarreront quand même, mais les fonctionnalités de notification et facturation automatique seront inactives.

### Étape 5 — Lancer les services Backend (9 terminaux)

> **Ordre obligatoire :** Eureka Server → API Gateway → Les 5 microservices → AI Service.
> Attendre que chaque service affiche `Started XxxApplication in X seconds` avant de lancer le suivant.
> **Rappel :** Définir les variables d'environnement de l'Étape 3 dans **chaque nouveau terminal**.

**Terminal 1 — Eureka Server :**
```bash
cd backend/eureka-server
mvn spring-boot:run
```
Vérification : ouvrir http://localhost:8761

---

**Terminal 2 — API Gateway :**
```bash
cd backend/api-gateway
mvn spring-boot:run
```

---

**Terminal 3 — Agenda Service :**
```bash
cd backend/agenda-service
mvn spring-boot:run
```

---

**Terminal 4 — Patient Service :**
```bash
cd backend/patient-service
mvn spring-boot:run
```

---

**Terminal 5 — Practitioner Service :**
```bash
cd backend/practitioner-service
mvn spring-boot:run
```

---

**Terminal 6 — Messaging Service :**
```bash
cd backend/messaging-service
mvn spring-boot:run
```

---

**Terminal 7 — Payment Service :**
```bash
cd backend/payment-service
mvn spring-boot:run
```

---

**Terminal 8 — AI Service (Python) :**

```bash
cd backend/ai-service

# Créer un environnement virtuel Python
python -m venv venv

# Activer l'environnement virtuel
# Windows PowerShell :
.\venv\Scripts\Activate.ps1
# Windows CMD :
# venv\Scripts\activate.bat
# Linux / Mac :
# source venv/bin/activate

# Installer les dépendances
pip install -r requirements.txt

# Lancer le service
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

> **Clé Gemini (optionnelle) :** Pour activer le moteur IA Gemini, définir la variable avant le lancement :
> ```powershell
> # PowerShell
> $env:GEMINI_API_KEY = "VOTRE_CLE_ICI"
> ```
> ```bash
> # Bash
> export GEMINI_API_KEY="VOTRE_CLE_ICI"
> ```
> Pour obtenir une clé : [Google AI Studio — API Keys](https://aistudio.google.com/app/apikey).
> Sans cette clé, le service utilise un moteur de règles locales automatiquement.

### Étape 6 — Lancer le Frontend Angular

**Terminal 9 :**
```bash
cd frontend
npm install
ng serve
```

L'application est accessible sur : **http://localhost:4200**

> En mode développement, Angular utilise un proxy (`proxy.conf.json`) pour router les appels API directement vers chaque service sans passer par l'API Gateway.

## 💾 Bases de Données

Le projet utilise **5 bases de données PostgreSQL distinctes** pour garantir l'isolation des données par microservice :

| Base de données | Service | Port |
|---|---|---|
| `agenda_medical` | agenda-service | 8081 |
| `patient_medical` | patient-service | 8082 |
| `practitioner_medical` | practitioner-service | 8083 |
| `messaging_medical` | messaging-service | 8084 |
| `payment_medical` | payment-service | 8085 |

**Paramètres de connexion par défaut :**
- **Hôte :** `localhost:5432`
- **Utilisateur :** `postgres`
- **Mot de passe :** `odoo`

> Avec Docker, les 5 bases sont créées automatiquement via le script `postgres-init/init-databases.sql`.

---

## 🛰️ Vérification Kafka

Pour vérifier que les événements de rendez-vous transitent correctement sur le topic `appointment-events` :

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic appointment-events \
  --from-beginning
```

Chaque réservation, modification ou mise à jour de statut de rendez-vous génère un événement JSON consommé par `messaging-service` (notifications) et `payment-service` (facturation).

---
