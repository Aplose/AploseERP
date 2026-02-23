# Documentation développeur

Cette section décrit l’architecture et la structure du projet AploseERP pour les développeurs qui souhaitent contribuer ou étendre l’application.

## Contenu

- **Architecture** : stack technique, structure des packages, multi-tenant
- **Modules** : extension par modules (à venir)

## Stack

- **Backend** : Spring Boot, Spring Security, Spring Data JPA
- **Front** : Thymeleaf, Bootstrap 5, WebJars
- **Base** : MariaDB (ou H2 en dev), Flyway pour les migrations

## Lancer le projet

- Prérequis : JDK 21, Maven
- Profil par défaut : `dev` (H2 en mémoire)
- Commande : `mvn spring-boot:run`

## Contributions

Le code source est disponible sur [GitHub](https://github.com/Aplose/AploseERP). Consultez le README et les issues pour les conventions de code et les PR.
