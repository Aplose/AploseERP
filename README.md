# AploseERP

**AploseERP** est un ERP open source multi-tenant : CRM (tiers, contacts), commerce (devis, commandes, factures), catalogue (produits, devises), projets, agenda, et modules no-code extensibles.  
Licence **GPL v3** — voir [LICENSE](LICENSE).

---

## Essayer gratuitement en ligne

Vous pouvez tester AploseERP sans rien installer, en accédant à l’instance hébergée :

- **https://erp.aplose.fr**

Créez un compte (inscription par entreprise), explorez les modules et l’interface. Aucune carte bancaire requise.

---

## Prérequis pour une installation autohébergée

- **Java 21** (JDK ou JRE)
- **MariaDB 10.6+** (base de données de production)
- **Maven 3.9+** (pour compiler depuis les sources) ou un JAR pré-buildé

Optionnel : reverse proxy (Nginx, Caddy) pour HTTPS et nom de domaine.

---

## Installation sur un serveur (autohébergé)

### 1. Cloner le dépôt et préparer l’environnement

```bash
git clone https://github.com/Aplose/AploseERP.git
cd AploseERP
```

### 2. Base de données MariaDB

Créez une base et un utilisateur dédiés :

```bash
sudo mysql -e "CREATE USER 'aploseErp'@'%' IDENTIFIED BY 'VOTRE_MOT_DE_PASSE';"
sudo mysql -e "CREATE DATABASE aploseErp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "GRANT ALL PRIVILEGES ON aploseErp.* TO 'aploseErp'@'%';"
sudo mysql -e "FLUSH PRIVILEGES;"
```

Remplacez `VOTRE_MOT_DE_PASSE` par un mot de passe fort.

### 3. Variables d’environnement (production)

L’application utilise le profil Spring `prod` en production. Définissez au minimum :

| Variable       | Description                    | Exemple              |
|----------------|--------------------------------|----------------------|
| `DB_HOST`      | Hôte MariaDB                   | `localhost`          |
| `DB_PORT`      | Port MariaDB                   | `3306`               |
| `DB_NAME`      | Nom de la base                 | `aploseErp`          |
| `DB_USER`      | Utilisateur MariaDB            | `aploseErp`          |
| `DB_PASSWORD`  | Mot de passe MariaDB           | *(votre mot de passe)* |

Optionnel pour l’envoi d’emails (inscription, etc.) :

- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`  
- Ou surcharge dans un fichier `application-prod.yml` local (hors dépôt).

### 4. Compilation et exécution

**Avec Maven (depuis les sources) :**

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=aploseErp
export DB_USER=aploseErp
export DB_PASSWORD='VOTRE_MOT_DE_PASSE'

mvn clean package -DskipTests
java -jar target/AploseERP-1.0.0-SNAPSHOT.jar
```

L’application écoute par défaut sur le **port 8080**. Ouvrez `http://VOTRE_SERVEUR:8080`.

**Lancer en arrière-plan (exemple avec `nohup`) :**

```bash
nohup java -jar target/AploseERP-1.0.0-SNAPSHOT.jar > /var/log/aploseErp/app.log 2>&1 &
```

### 5. (Recommandé) Service systemd

Créez `/etc/systemd/system/aplose-erp.service` :

```ini
[Unit]
Description=AploseERP
After=network.target mariadb.service

[Service]
Type=simple
User=aplose
WorkingDirectory=/opt/aplose-erp
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DB_HOST=localhost"
Environment="DB_PORT=3306"
Environment="DB_NAME=aploseErp"
Environment="DB_USER=aploseErp"
EnvironmentFile=-/etc/aplose-erp/env
ExecStart=/usr/bin/java -Xmx512m -jar /opt/aplose-erp/AploseERP-1.0.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

- Placez le JAR dans `/opt/aplose-erp/`.
- Optionnel : créez `/etc/aplose-erp/env` avec `DB_PASSWORD=...` (chmod 600) et référencez-le avec `EnvironmentFile=-/etc/aplose-erp/env`.
- Puis :

```bash
sudo systemctl daemon-reload
sudo systemctl enable aplose-erp
sudo systemctl start aplose-erp
sudo systemctl status aplose-erp
```

### 6. Reverse proxy HTTPS (exemple Nginx)

Pour exposer l’application en HTTPS avec un nom de domaine :

```nginx
server {
    listen 443 ssl http2;
    server_name erp.votredomaine.fr;

    ssl_certificate     /etc/letsencrypt/live/erp.votredomaine.fr/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/erp.votredomaine.fr/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_redirect off;
    }
}
```

Configurez Spring pour faire confiance aux en-têtes de forwarding (déjà le cas en `prod` avec `forward-headers-strategy: NATIVE` et cookies sécurisés).

### 7. Premier accès

1. Ouvrez l’URL de l’application (ex. `https://erp.votredomaine.fr`).
2. Cliquez sur **S’inscrire** / **Créer un compte**.
3. Renseignez les informations de votre entreprise et créez un compte administrateur.
4. Connectez-vous et configurez les modules dans **Admin → Modules** et, si besoin, **Admin → Catalogue de modules** pour les applications no-code.

---

## Développement local

- **Profil** : `dev` (base H2 en fichier, pas de MariaDB requis).
- **Lancement** : `mvn spring-boot:run` (ou exécuter `ErpApplication` depuis l’IDE).
- **Interface H2** : `http://localhost:8080/h2-console` (JDBC URL : `jdbc:h2:file:./data/aploseErpDev`, user/password : `aploseErp`).

---

## Documentation

- **Utilisateur** : [Documentation en ligne](https://erp.aplose.fr/docs) (ou `/docs` sur votre instance).
- **Code** : voir [src/main/resources/docs/](src/main/resources/docs/) (Markdown).

---

## Contribution et code source

- Dépôt : **https://github.com/Aplose/AploseERP**
- Licence : **GPL v3** — voir [LICENSE](LICENSE).  
- Modifications et redistributions doivent respecter la GPL v3 et inclure la licence et les notices de copyright.

---

## Résumé des commandes utiles

| Action              | Commande |
|---------------------|----------|
| Compiler            | `mvn clean compile` |
| Créer le JAR        | `mvn clean package -DskipTests` |
| Lancer (dev)        | `mvn spring-boot:run` |
| Tests               | `mvn test` |
| Lancer le JAR prod  | `java -jar target/AploseERP-1.0.0-SNAPSHOT.jar` (avec variables d’env `prod` et MariaDB) |

Pour **tester sans installer**, utilisez l’instance en ligne : **https://erp.aplose.fr**.
