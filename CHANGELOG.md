# Changelog

All notable changes to AploseERP are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Le dossier de build Maven `target/` n'est plus suivi par git (ajouté au `.gitignore`).
- Profil `prod` : plus de mot de passe par défaut pour la base de données ; `DB_PASSWORD` doit être défini, sinon l'application refuse de démarrer.
- Documentation : le compte de démonstration `admin` est précisé comme réservé au profil `dev`.

## [1.0.0-SNAPSHOT] – 2026-02

### Added

- **Multi-tenant ERP** : inscription par entreprise (tenant), isolation des données par tenant.
- **CRM** : tiers (sociétés), contacts, avec champs complémentaires configurables.
- **Commerce** : devis, commandes, factures, suivi des paiements.
- **Catalogue** : produits, devises.
- **Projets** : projets et tâches, statuts et avancement.
- **Agenda** : événements et calendrier partagé.
- **Configuration des modules par tenant** : activation/désactivation des blocs (CRM, Commerce, Catalogue, Projets, Agenda) depuis l’admin ; menu et accès aux URLs adaptés.
- **Objets métier** : registre unifié (BusinessObjectRegistry, EntityFieldRegistry) pour permissions, champs complémentaires et menu.
- **Modules no-code** : définitions de modules (ex. « Gestion de ferme ») et d’entités personnalisées (ex. Animal) ; catalogue de modules, activation par tenant, listes/formulaires/détails génériques, menu Applications.
- **Documentation** : pages utilisateur, intégrateur et développeur (Markdown).
- **Sécurité** : rôles, permissions granulaires, sessions sécurisées, envoi d’emails (inscription, rappels).
- **Internationalisation** : français et anglais, messages surchargeables en base.

### Technical

- Stack : Java 21, Spring Boot 4, Thymeleaf, JPA/Hibernate, Flyway, MariaDB (prod) / H2 (dev).
- Build : Maven ; exécution en JAR ou via `mvn spring-boot:run`.

---

[Unreleased]: https://github.com/Aplose/AploseERP/compare/HEAD
[1.0.0-SNAPSHOT]: https://github.com/Aplose/AploseERP/releases
