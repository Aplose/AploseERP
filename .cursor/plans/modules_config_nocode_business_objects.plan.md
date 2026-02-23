---
name: ""
overview: ""
todos: []
isProject: false
---

# Plan : Configuration des modules, objets métier et modules no-code

## Contexte actuel

- **Menu** : sidebar dans [layout/base.html](src/main/resources/templates/layout/base.html) avec entrées fixes (CRM, Commerce, Catalogue, Projets, Admin). Chaque entrée est protégée par `sec:authorize="hasAuthority('XXX_READ')"`.
- **Permissions** : table `permissions` (code, module, action), liées aux rôles. Modules actuels : USER, ROLE, TENANT, THIRD_PARTY, CONTACT, PRODUCT, PROPOSAL, SALES_ORDER, INVOICE, PAYMENT, CURRENCY, PROJECT, TASK, AGENDA, etc.
- **Entités** : toutes héritent de [BaseEntity](src/main/java/fr/aplose/erp/core/entity/BaseEntity.java) (id, tenantId, createdAt, updatedAt). Pas de notion explicite d’« objet métier » au niveau modèle.
- **Champs complémentaires** : [EntityFieldRegistry](src/main/java/fr/aplose/erp/modules/extrafield/service/EntityFieldRegistry.java) référence des types d’entités (THIRD_PARTY, CONTACT, PRODUCT, PROPOSAL, INVOICE) en dur.
- **Aucune configuration par tenant** pour activer/désactiver des blocs fonctionnels (tiers, facture, projet, etc.).

---

## 1. Objectifs

1. **Configuration des modules par tenant** : les administrateurs de chaque tenant peuvent activer/désactiver des « modules » (grosses fonctionnalités). Si un module est désactivé, l’entrée de menu correspondante disparaît et l’accès aux URLs est refusé ou redirigé.
2. **Notion d’objet métier** : introduire une classe (ou interface) mère / un registre d’« objets métier » (facture, devis, tiers, contact, projet, etc.) pour unifier le référencement (permissions, extra fields, menu, et plus tard no-code).
3. **Modules no-code** : permettre de définir des modules (déclarés par un tenant ou la plateforme) qui ajoutent de **nouveaux objets métiers** (ex. « Animal » pour un module ferme). Ces modules sont partageables : un tenant peut publier un module, d’autres peuvent l’activer et l’utiliser. Architecture modulable et orientée no-code (définition en données, pas seulement en code).

---

## 2. Configuration des modules (activation par tenant)

### 2.1 Modèle de données

- **Table `tenant_modules**` (ou `tenant_feature_flags`) :
  - `tenant_id` (FK → tenants)
  - `module_code` (VARCHAR) : ex. `CRM_THIRD_PARTY`, `CRM_CONTACT`, `COMMERCE_PROPOSAL`, `COMMERCE_ORDER`, `COMMERCE_INVOICE`, `CATALOG_PRODUCT`, `CATALOG_CURRENCY`, `PROJECT`, `AGENDA`
  - `enabled` (BOOLEAN, défaut true)
  - `settings` (JSON/JSONB ou VARCHAR optionnel) pour paramètres futurs (ex. limite de devis/an pour Découverte)
  - Contrainte unique (tenant_id, module_code)
- **Définition des « modules » côté applicatif** : une liste statique (enum ou registre) des modules « core » avec :
  - code (aligné sur les permissions existantes ou regroupé : ex. THIRD_PARTY → module_code `CRM_THIRD_PARTY`)
  - libellé i18n, icône, ordre d’affichage
  - lien vers l’entrée de menu (path, permission minimale READ)
- **Comportement** : à la création d’un tenant, insérer une ligne par module core avec `enabled = true` (ou ne pas insérer = considérer « activé par défaut »). L’admin peut désactiver un module ; alors :
  - le menu n’affiche plus l’entrée (lecture de la config dans le contrôleur ou un `@ControllerAdvice` qui expose les modules activés au modèle).
  - les contrôleurs des URLs concernées vérifient que le module est activé (sinon 403 ou redirection vers dashboard avec message).

### 2.2 Couche service et API

- **TenantModuleService** (ou TenantFeatureService) :
  - `isModuleEnabled(tenantId, moduleCode): boolean`
  - `getEnabledModuleCodes(tenantId): Set<String>`
  - `updateModuleEnabled(tenantId, moduleCode, boolean)` (réservé admin)
  - `getModuleSettings(tenantId, moduleCode)` / `updateModuleSettings(...)` (optionnel phase 1)
- Utilisation dans un **ControllerAdvice** (ex. existant ou nouveau) : ajouter au modèle global (pour les vues Thymeleaf) un attribut `enabledModules` (Set des codes activés) pour que la sidebar n’affiche que les entrées dont le module est activé **et** l’utilisateur a la permission.

### 2.3 Menu dynamique

- **Layout base.html** : au lieu d’afficher chaque lien en dur avec `sec:authorize`, combiner :
  - `sec:authorize="hasAuthority('THIRD_PARTY_READ')"` **et** vérification que le module `CRM_THIRD_PARTY` (ou le code associé) est dans `enabledModules`.
- Option : extraire la structure du menu dans un fragment ou un composant qui reçoit la liste des entrées à afficher (calculée en backend : modules activés ∩ permissions utilisateur).

### 2.4 Écran d’administration

- **Paramètres → onglet « Modules »** (ou page dédiée « Activation des modules ») :
  - Liste des modules core avec libellé, description courte, interrupteur activé/désactivé.
  - Sauvegarde en base (`tenant_modules`). Accès réservé TENANT_READ / TENANT_UPDATE (ou rôle admin).

### 2.5 Sécurité et contrôleurs

- Pour chaque contrôleur de module (ThirdParty, Contact, Proposal, Invoice, Project, Agenda, etc.) : en plus de `@PreAuthorize`, vérifier au début des méthodes importantes (ou via un filtre/interceptor) que le module est activé pour le tenant courant. Sinon retourner 403 ou rediriger.

---

## 3. Notion d’objet métier (classe mère / registre)

### 3.1 Objectif

- Unifier le référencement des « types d’entités métier » : permissions (module/action), champs complémentaires (EntityFieldRegistry), menu, et plus tard no-code (nouveaux types déclarés par des modules).
- Permettre à un module no-code de déclarer un nouveau type (ex. ANIMAL) sans coder une entité JPA dédiée (voir section 4).

### 3.2 Approche proposée : registre + interface optionnelle

- **BusinessObjectType** (registre central) :
  - Enum ou classe « registry » listant les types d’objets métier **core** : THIRD_PARTY, CONTACT, PRODUCT, PROPOSAL, SALES_ORDER, INVOICE, PAYMENT, PROJECT, PROJECT_TASK, AGENDA_EVENT, CURRENCY, etc.
  - Pour chaque type : code (String), libellé i18n, icône (Bootstrap icon), entité JPA associée si applicable (Class<? extends BaseEntity>), permission READ minimale, module_code pour liaison avec la config d’activation.
- **Interface marqueur (optionnelle)** : `BusinessObject` ou `BusinessEntity` que les entités métier peuvent implémenter, avec une méthode `String getBusinessObjectTypeCode()`. Cela permet au code de retrouver le type à partir d’une entité. Les entités existantes (ThirdParty, Invoice, …) peuvent être progressivement marquées ou le type peut être dérivé du nom de la classe / table.
- **Classe mère** : `BaseEntity` reste la base JPA. Une éventuelle couche « BusinessEntity » au-dessus pourrait ajouter par exemple un `getObjectTypeCode()` par défaut selon la classe, mais le plus simple en phase 1 est de garder `BaseEntity` et de faire reposer la notion d’objet métier sur le **registre** (pas d’héritage supplémentaire obligatoire). Les entités core restent telles quelles ; le registre associe ThirdParty → THIRD_PARTY, Invoice → INVOICE, etc.
- **EntityFieldRegistry** : faire dépendre la liste des types d’entités du registre « business object » (core + custom) au lieu d’une liste en dur, pour préparer l’ajout de types custom.

### 3.3 Récapitulatif

- Pas de nouvelle classe mère JPA obligatoire : on s’appuie sur `BaseEntity` et un **registre d’objets métier** (core + extension no-code).
- Le registre permet de :
  - savoir quels types existent ;
  - pour chaque type, quelle permission, quel module_code, quelle entité (si core) ou quelle définition (si custom).

---

## 4. Modules no-code : architecture modulable

### 4.1 Principes

- Un **module no-code** est une **définition** (métadonnées) déclarant :
  - un identifiant (code, version), un nom, une description, un tenant auteur (optionnel) ;
  - un ou plusieurs **objets métier personnalisés** (ex. Animal), chacun avec : code, libellé, liste de champs (nom, type, obligatoire, etc.), colonnes de liste, options d’affichage.
- Aucune nouvelle table JPA « métier » en code pour ces objets : la structure est décrite en JSON (ou en base dans des tables de définition), et les **données** sont stockées soit en table générique (EAV ou JSON), soit en tables créées dynamiquement au moment de l’activation du module (schema-on-write).
- **Partage** : un tenant (ou la plateforme) crée une définition de module et peut la **publier** (marché interne ou catalogue). Un autre tenant peut **installer/activer** le module : une copie de la définition est associée au tenant (tenant_module) et le stockage des données pour les objets custom est créé ou utilisé.

### 4.2 Modèle de données proposé

- **module_definitions** (définitions de modules, partageables) :
  - id, code (ex. `farm`), name, description, version (ex. 1.0.0)
  - author_tenant_id (NULL = plateforme / core), is_public (boolean), created_at, updated_at
  - schema_json (JSON) : décrit les business objects du module (voir ci-dessous).
- **tenant_modules** (déjà prévu pour activation des modules core) : étendre pour supporter aussi les modules no-code :
  - tenant_id, module_code (pour core : CRM_THIRD_PARTY ; pour no-code : ex. `module:farm` ou référence à module_definitions.id)
  - enabled, settings_json (paramètres optionnels), activated_at
  - Pour les modules no-code : module_definition_id (FK optionnelle vers module_definitions).
- **custom_entity_definitions** (objets métier custom, par module) :
  - id, module_definition_id (ou dérivé du schema du module), code (ex. ANIMAL), name, description
  - fields_schema (JSON) : liste de champs [{ "name", "type", "required", "label", "options"?, ... }]
  - list_columns (JSON) : colonnes à afficher en liste
  - Contrainte unique (module_definition_id, code).
- **Stockage des données des objets custom** :
  - **Option A (recommandée pour v1)** : une table générique **custom_entity_data** avec colonnes : id, tenant_id, entity_definition_id (ou module_code + object_code), payload (JSONB). Requêtes par tenant + type ; index GIN sur payload si besoin de filtres. Simple, pas de migration par type.
  - **Option B** : à l’activation du module, création dynamique de tables (ex. `custom_animal_<tenant_id>`) à partir du schema. Plus performant pour gros volumes et requêtes SQL classiques, mais plus complexe (migrations dynamiques, gestion des évolutions de schema).

Recommandation : **Option A** pour la première itération (no-code v1), avec possibilité d’évoluer vers Option B plus tard pour des modules « lourds ».

### 4.3 Descriptor de module (JSON)

Exemple pour un module « Ferme » :

```json
{
  "code": "farm",
  "name": "Gestion de ferme",
  "version": "1.0.0",
  "businessObjects": [
    {
      "code": "ANIMAL",
      "label": "Animal",
      "icon": "bi-egg",
      "fields": [
        { "name": "name", "type": "string", "label": "Nom", "required": true },
        { "name": "species", "type": "string", "label": "Espèce" },
        { "name": "birthDate", "type": "date", "label": "Date de naissance" }
      ],
      "listColumns": ["name", "species", "birthDate"]
    }
  ]
}
```

Les types de champs supportés en v1 : string, text, number, date, boolean, reference (lien vers un autre objet métier, ex. ThirdParty). Extensions possibles : list, file.

### 4.4 Couche applicative

- **ModuleDefinitionService** : CRUD des définitions (création par un tenant, publication, liste des modules disponibles pour un tenant).
- **TenantModuleService** (étendu) : activer/désactiver un module (core ou no-code) pour un tenant ; à l’activation d’un module no-code, enregistrer les custom_entity_definitions et préparer le stockage (pas de création de table en Option A).
- **CustomEntityService** : lecture/écriture des instances d’objets custom (liste, détail, création, mise à jour, suppression) basée sur entity_definition_id + tenant_id, stockage en custom_entity_data (payload JSON).
- **BusinessObjectRegistry** (étendu) : en plus des types core, charger les types issus des custom_entity_definitions activés pour le tenant courant ; exposer une liste unifiée de « business object types » (code, label, icon, isCustom, definitionId).

### 4.5 Contrôleur générique et menu dynamique

- **CustomEntityController** (ou NoCodeController) :
  - Routes génériques : `GET /app/{moduleCode}/{objectCode}` (liste), `GET /app/{moduleCode}/{objectCode}/new`, `POST /app/{moduleCode}/{objectCode}`, `GET /app/{moduleCode}/{objectCode}/{id}`, `GET /app/{moduleCode}/{objectCode}/{id}/edit`, `PUT`, `DELETE`.
  - Résolution du module et de l’objet à partir de moduleCode et objectCode ; vérification que le module est activé et que l’utilisateur a la permission (générique ou par type custom).
  - Permissions pour objets custom : soit un droit générique (ex. `CUSTOM_ENTITY_READ`, `CUSTOM_ENTITY_CREATE`, … avec scope module/object), soit création dynamique de permissions en base à l’activation du module (ex. `FARM_ANIMAL_READ`, …) et attribution aux rôles par l’admin.
- **Menu** : les entrées pour les objets custom sont ajoutées dynamiquement (section « Applications » ou « Modules ») : pour chaque module no-code activé et chaque business object, une entrée avec lien vers `/app/{moduleCode}/{objectCode}`. Affichage conditionnel selon permission.

### 4.6 Partage et catalogue

- **Publication** : un tenant avec un module défini (module_definitions avec author_tenant_id = ce tenant) peut marquer `is_public = true`. Les définitions publiques sont listées dans un « catalogue de modules » (page dédiée ou onglet dans Paramètres).
- **Installation** : un autre tenant choisit un module du catalogue et clique « Activer » : création d’une entrée tenant_modules (avec module_definition_id), copie ou référence des custom_entity_definitions pour ce tenant. Les données restent propres au tenant (custom_entity_data filtré par tenant_id).

### 4.7 Sécurité

- Vérifier que toute requête sur un objet custom est bien limitée au tenant courant et au module activé.
- Contrôle d’accès : soit permissions dédiées par type custom (créées à l’activation), soit un rôle « utilisateur de modules custom » avec droit générique CUSTOM_ENTITY_*.

---

## 5. Synthèse des tâches techniques (ordre suggéré)


| Phase | Domaine        | Tâches                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ----- | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1** | Config modules | 1) Migration : table `tenant_modules` (tenant_id, module_code, enabled, settings_json). 2) Enum ou registre des modules core (code, label, permission READ, path menu). 3) TenantModuleService (isEnabled, getEnabled, update). 4) ControllerAdvice : expositions `enabledModules` au modèle. 5) Adapter base.html : afficher une entrée de menu seulement si module activé et permission OK. 6) Écran Admin « Modules » : liste + toggles. 7) Dans chaque contrôleur concerné (ou interceptor), vérifier module activé.                                                                                                                                                                                                      |
| **2** | Objets métier  | 1) Créer BusinessObjectType (registre) : codes core, label, icon, permission, module_code, entityClass optionnel. 2) Optionnel : interface BusinessObject sur entités + getBusinessObjectTypeCode(). 3) Faire évoluer EntityFieldRegistry pour s’appuyer sur ce registre (core). 4) Documenter le fait que les types custom (no-code) seront ajoutés au registre dynamiquement.                                                                                                                                                                                                                                                                                                                                               |
| **3** | No-code (v1)   | 1) Migrations : module_definitions, custom_entity_definitions, custom_entity_data (id, tenant_id, entity_definition_id, payload JSONB). 2) Étendre tenant_modules (module_definition_id, pour modules no-code). 3) ModuleDefinitionService, CustomEntityService, extension BusinessObjectRegistry (types custom). 4) Descriptor JSON et parsing. 5) CustomEntityController (routes /app/{moduleCode}/{objectCode}/...). 6) Permissions : génériques CUSTOM_ENTITY_* ou création à l’activation. 7) Menu dynamique : entrées pour modules no-code activés. 8) UI générique : liste (colonnes configurées), formulaire (champs du schema), détail. 9) Catalogue : liste des modules publics, bouton « Activer » pour un tenant. |


---

## 6. Schémas de flux

### 6.1 Affichage du menu (après phase 1)

```
Utilisateur connecté → ControllerAdvice charge enabledModules (tenant) + permissions (user)
→ Template reçoit enabledModules et authorities
→ Pour chaque entrée menu : afficher si (module in enabledModules) et (user a permission READ)
```

### 6.2 Activation d’un module no-code (phase 3)

```
Admin choisit « Activer » sur un module du catalogue
→ TenantModuleService.activateModule(tenantId, moduleDefinitionId)
→ Création tenant_modules + enregistrement custom_entity_definitions
→ (Optionnel) Création de permissions pour les types du module et attribution au rôle admin
→ Menu recalculé : nouvelles entrées pour les business objects du module
```

### 6.3 Création d’un enregistrement « Animal » (no-code)

```
POST /app/farm/ANIMAL avec { name, species, birthDate }
→ CustomEntityController résout module farm + object ANIMAL
→ Vérification module activé + permission
→ CustomEntityService.create(tenantId, definitionId, payload)
→ INSERT dans custom_entity_data (tenant_id, entity_definition_id, payload)
```

---

## 7. Références

- Menu actuel : [layout/base.html](src/main/resources/templates/layout/base.html) (sidebar avec sec:authorize).
- Permissions : [V2__security.sql](src/main/resources/db/migration/V2__security.sql), [Permission](src/main/java/fr/aplose/erp/security/entity/Permission.java).
- Base entités : [BaseEntity](src/main/java/fr/aplose/erp/core/entity/BaseEntity.java).
- Champs complémentaires / types d’entités : [EntityFieldRegistry](src/main/java/fr/aplose/erp/modules/extrafield/service/EntityFieldRegistry.java).

---

## 8. Notes d’implémentation

- **Performance** : pour le menu, le set des modules activés peut être mis en cache par tenant (Caffeine/Redis) avec invalidation à la sauvegarde de la config.
- **i18n** : les libellés des modules core et des champs no-code peuvent être des clés de messages (module.label.farm, animal.label.name, etc.) ou du texte stocké dans la définition.
- **Évolutions** : ajout de relations entre objets custom (reference vers ThirdParty, vers un autre custom), workflows simples (statuts), et à long terme exécution de règles ou scripts (nocode avancé) pourront s’appuyer sur ce socle.

