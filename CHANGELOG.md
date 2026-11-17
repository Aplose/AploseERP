# Changelog

All notable changes to AploseERP are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed — accès unique et international

- **Un seul accès pour toutes les entreprises** : plus de sous-domaine par entreprise. On se connecte avec son email (ou identifiant) ; l'entreprise est celle du compte. Email et identifiant sont uniques sur toute la plateforme. Le premier compte d'une entreprise en est l'administrateur et crée les autres comptes.
- Formulaires publics : adresse `/t/{code entreprise}/form/{code}` (l'adresse `/form/{code}` sans entreprise ne fonctionne plus).
- **Pays** : les 249 pays ISO 3166, nommés dans la langue de l'utilisateur (inscription, tiers, contacts, paramètres). Le pays devient obligatoire à l'inscription.
- **Devises** : toutes les devises ISO 4217 en usage (migration V15) ; devise, langue et fuseau horaire de l'entreprise choisis à l'inscription ou déduits du pays. Les nouveaux documents prennent la devise de l'entreprise (au lieu de l'euro).
- **Langue** : interface dans la langue de l'utilisateur, sinon celle du navigateur ; emails dans la langue du destinataire (modèles anglais ajoutés, migration V16).

### Security

- Isolation entre entreprises : une fois connecté, l'entreprise courante est toujours celle de l'utilisateur. Auparavant, un sous-domaine ou un en-tête `X-Tenant-ID` donnait accès aux données d'une autre entreprise (#3).
- Formulaires : `id`, `tenantId` et les champs techniques ne sont plus lus depuis la requête. Auparavant, un formulaire de création portant l'identifiant d'un enregistrement existant l'écrasait, y compris dans une autre entreprise (#3).

### Added

- Création d'une facture depuis une commande (bouton « Créer la facture ») ; une commande n'est facturée qu'une fois (#3).
- Import Dolibarr : projets et tâches (sous-tâches comprises) vers le module projets (#2).
- Tests : isolation entre entreprises, circuit devis → commande → facture → paiements, validation du schéma, import Dolibarr (24 tests).

### Fixed

- Toutes les pages en `layout:decorate` renvoyaient une erreur 500 (Groovy 5 incompatible avec thymeleaf-layout-dialect : Groovy 4 imposé) (#3).
- Pages de détail en erreur sur les relations (`open-in-view` réactivé) (#3).
- Le profil `prod` refusait de démarrer (`ddl-auto=validate`) : colonnes manquantes et types différents, corrigés par la migration V14 (#3).
- Authentification par clé API en erreur (`LazyInitializationException`) (#3).
- Un devis converti en commande passe à `CONVERTED` et ne peut plus être converti deux fois (#3).
- Import Dolibarr (#2) : pagination complète (plus de troncature à 5 000-10 000 enregistrements) ; relance sans doublons ni perte de liens ; paiements lus par facture ; dates en timestamp Unix ; montant payé ; avoirs gardés en ventes ; statuts alignés ; TVA des produits en fraction ; un enregistrement rejeté ne fait plus échouer tout l'import ; paramètres de connexion isolés par import.

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
