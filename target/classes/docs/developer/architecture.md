# Architecture

## Structure des packages

- **config** : configuration Spring (sécurité, locale, cache)
- **core** : contrôleurs publics, erreurs, utilitaires
- **tenant** : entités et services liés au multi-tenant (Tenant, User, inscription, formules)
- **modules** : fonctionnalités métier (CRM, catalogue, commerce, projets, agenda, admin)

Chaque module peut contenir des entités, repositories, services et contrôleurs. Les vues Thymeleaf sont dans `resources/templates/modules/`.

## Multi-tenant

L’application est multi-tenant : chaque client (entreprise) dispose d’un **tenant** avec des utilisateurs et des données isolées. Le tenant est déterminé par le sous-domaine ou par un identifiant en en-tête/session selon la configuration.

Les entités métier sont généralement liées à un `tenantId` pour le filtrage des données.

## Rôles

- **SUPER_ADMIN** : administration globale (tous les tenants)
- **TENANT_ADMIN** : administration du tenant (utilisateurs, paramètres, dictionnaires)
- **USER** : utilisateur standard (accès aux modules métier selon droits)

## Sécurité

- Spring Security avec formulaire de connexion
- Les routes publiques (`/`, `/login`, `/signup`, `/docs`, etc.) sont en `permitAll()`
- Les routes `/admin/**` nécessitent un rôle admin

## Base de données

- Migrations Flyway dans `src/main/resources/db/migration`
- Entités JPA avec schéma commun ; le filtrage tenant se fait au niveau service/repository
