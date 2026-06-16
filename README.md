# 🏥 Mediconnect - Plateforme de Gestion Médicale Moderne

Mediconnect est une solution complète de gestion de cabinet médical et de prise de rendez-vous en ligne, conçue avec une architecture microservices robuste et une interface utilisateur réactive et haute performance.

## 🛠️ Stack Technique

- **Backend :** Java 21, Spring Boot 3.4.2, Spring Cloud (Eureka Discovery Server, Cloud Gateway), Spring Security (JWT), Hibernate/JPA, OpenFeign.
- **Message Broker :** Apache Kafka 3.7.0 (mode KRaft - sans Zookeeper).
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

Pour démarrer en local sans Docker, commencez par lancer le serveur Eureka, puis la Gateway, puis les microservices :

```bash
# Lancer le serveur d'enregistrement (Eureka Server)
cd backend/eureka-server
mvn spring-boot:run

# Lancer la Gateway (API Gateway)
cd backend/api-gateway
mvn spring-boot:run

# Lancer les autres microservices (agenda-service, patient-service, practitioner-service, messaging-service, payment-service) :
cd backend/<service-folder>
mvn spring-boot:run
```

**Services et Ports :**
- `eureka-server` : **8761** (Serveur de découverte / Annuaire des services)
- `api-gateway` : **8090** (Passerelle API / Routage)
- `agenda-service` : **8081** (Cœur du système)
- `patient-service` : **8082** (Gestion des patients & comptes)
- `practitioner-service` : **8083** (Profils praticiens & authentification pro)
- `messaging-service` : **8084** (Messagerie sécurisée inter-services)
- `payment-service` : **8085** (Gestion des paiements)

### 2. Démarrage du Frontend

```bash
cd frontend
npm install
ng serve
```
L'application sera accessible sur `http://localhost:4200` et passera par l'API Gateway sur le port `8090` pour joindre les microservices.

## ✨ Fonctionnalités Clés

- **Architecture Microservices :** Découverte dynamique des services via Eureka Server et routage via API Gateway.
- **Agenda Dynamique :** Gestion des rendez-vous en temps réel avec indicateurs visuels.
- **Messagerie Sécurisée :** Échange de documents et messages avec résolution automatique des noms via communication inter-services.
- **Multi-Rôles :** Espaces dédiés pour les Praticiens, les Assistants et les Patients.
- **Design Premium :** Support complet du **Mode Sombre**, accessibilité avancée et animations fluides.
- **Zéro Rechargement (No F5) :** Gestion d'état moderne via **Angular Signals** pour une réactivité instantanée.

## 🐳 Déploiement avec Docker

Le projet contient une configuration complète pour déployer l'ensemble des services (y compris le broker Kafka) via **Docker Compose**.

### Lancement standard (Tous les services)
```bash
docker compose up -d
```
*(Ajoutez `--build` si vous venez de modifier du code Java ou Angular pour forcer la recompilation dans Docker).*

### Lancement partiel (Kafka et services concernés)
Si vous souhaitez déployer ou reconstruire uniquement le broker Kafka et les services connectés :
```bash
docker compose up -d --build kafka agenda-service messaging-service payment-service
```

### 🛰️ Vérification et Inspection du trafic Kafka
Pour vérifier en temps réel que les événements de rendez-vous transitent bien sur le topic `appointment-events`, exécutez le consommateur console à l'intérieur du conteneur Kafka :
```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic appointment-events --from-beginning
```

Chaque réservation, modification ou mise à jour de statut de rendez-vous génère un événement JSON qui sera consommé en direct par `messaging-service` (notifications) et `payment-service` (facturation).

---
