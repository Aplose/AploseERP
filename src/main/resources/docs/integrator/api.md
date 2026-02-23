# API REST

## Présentation

AploseERP expose des APIs REST pour intégrer des applications tierces. Les endpoints sont préfixés par le chemin de l’application et le contexte tenant lorsque applicable.

## Authentification

L’authentification repose sur la session (cookie) ou sur un mécanisme token selon la configuration. Pour les appels automatisés, un token ou une clé API pourra être fourni (à documenter selon l’évolution du produit).

## Exemples d’appels

Les principaux cas d’usage sont :

- Lecture / écriture de tiers et contacts
- Création de devis, commandes ou factures
- Consultation du catalogue produits

Les formats utilisés sont en général JSON pour les requêtes et réponses. Les en-têtes recommandés :

- `Content-Type: application/json`
- `Accept: application/json`

## Limites

Les limites de débit et les quotas seront documentés lors de la mise en place des APIs publiques. En cas de volume important, contacter l’équipe produit.
