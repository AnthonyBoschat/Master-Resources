# Séance 8 — Production, retour arrière et documentation

**Durée : 1 h 30.** **Livrable :** une production déployable à la demande, un rollback démontré, des sauvegardes, et une documentation reprenable par un collègue.

## Notions de la séance

- **Livraison continue ou déploiement continu** : en production, on choisit souvent une *porte manuelle* (un humain décide) plutôt qu'un déploiement automatique. Le job est `when: manual` et réservé à un environnement *protégé*.
- **Tags de version** : on déploie un tag (`v1.0.0`), jamais « la branche ». SemVer : `MAJEUR.MINEUR.CORRECTIF` ; une version publiée ne se modifie plus.
- **Rollback** : revenir en arrière, c'est redéployer l'image d'un tag précédent, sans toucher à l'historique git. Ça marche parce que les images sont **immuables**.
- **La limite du rollback** : il restaure le code, pas la donnée. Des documents supprimés ne reviennent pas ; des migrations de base peuvent ne pas être réversibles. D'où les **sauvegardes**.
- **Sauvegarde utile = sauvegarde restaurée** : une archive jamais restaurée est une hypothèse. On teste la restauration, on garde plusieurs versions (rétention) et on sait combien de données on peut perdre.
- **Trois documents, trois questions** : le *README* répond à « comment démarrer ? », le *runbook* à « que faire quand… ? » (déployer, diagnostiquer, revenir en arrière), l'*ADR* à « **pourquoi** a-t-on décidé cela ? ».
- **Un schéma vaut une page** : un diagramme (Mermaid, rendu par GitLab) montre en quelques secondes qui parle à qui.
- **Pour qui écrire** : pour un collègue qui reprend le projet dans six mois, ou pour vous-même. Des commandes copiables valent mieux que des généralités.
- **Noms de jobs imposés** : `deploy-production` et `rollback-production`.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 1 | 20 min | Production : un tag, un bouton, un environnement protégé |
| 2 | 20 min | Rollback : redéployer un tag précédent |
| 3 | 20 min | Sauvegarder et restaurer la donnée |
| 4 | 25 min | Documentation : README, runbook, ADR, schéma |
| 5 | 5 min | Vérification finale |

Créez une branche `s8-prod` à partir de `main`.

---

## Partie 1 — Production (20 min)

1. Créez dans votre projet les variables `APP_PORT_PROD` et `MONGODB_URI_PROD` (masquée).
2. Transformez le job de déploiement en **modèle réutilisable** (ancre YAML `.deploy`) : les jobs staging et production ne diffèrent que par leurs variables (`ENV_NAME`, `IMAGE_TAG`, port, URI Mongo, adresse).
3. Ajoutez `deploy-production` : déclenché **seulement sur un tag** (`rules: if: $CI_COMMIT_TAG`) et **à la main** (`when: manual`), qui déploie `IMAGE_TAG: $CI_COMMIT_TAG`. Environnement `production`, adresse `https://<identifiant>.kptn.fr`.
4. Dans **Settings → CI/CD → Protected environments**, restreignez `production` aux personnes autorisées. Cette fonction dépend de l'abonnement du groupe (Premium ou Ultimate) : si elle n'est pas disponible, notez-le ; la porte manuelle `when: manual` reste en place.
5. Créez le tag `v1.0.0` sur `main` et déployez.

- [ ] Le job `deploy-production` n'apparaît que sur les tags et demande un clic
- [ ] `https://<identifiant>.kptn.fr` répond ; `/health` est `ok`
- [ ] Vous savez expliquer la différence entre livraison continue et déploiement continu, et ce que vous avez choisi

## Partie 2 — Rollback (20 min)

1. Créez un second tag `v1.1.0` avec un changement visible (par exemple un texte du front) et déployez-le en production.
2. Ajoutez un job `rollback-production`, manuel, basé sur le même modèle, avec `IMAGE_TAG: $ROLLBACK_TAG`. Le modèle doit **échouer clairement** si `IMAGE_TAG` est vide.
3. Lancez-le (**Run job**) en renseignant `ROLLBACK_TAG=v1.0.0`.
4. Vérifiez que l'ancienne version est revenue.

- [ ] La production est revenue à `v1.0.0` sans nouveau commit
- [ ] Un lancement sans `ROLLBACK_TAG` échoue avec un message explicite

## Partie 3 — Sauvegarder et restaurer (20 min)

Revenir à une image précédente ne restaure pas des données supprimées.

1. Écrivez `scripts/backup-mongo.sh` : lit `MONGODB_URI` dans `~/moviejs-<env>/.env`, lance `mongodump --archive --gzip` **dans l'image `mongo:8.0`** (rien à installer sur le serveur), écrit dans `~/backups/`, garde les 7 dernières archives.
2. Écrivez `scripts/restore-mongo.sh` : demande confirmation, puis `mongorestore --archive --gzip --drop` depuis l'archive.
3. Copiez-les sur le serveur (`scp`) et testez : sauvegarde, suppression volontaire d'un film, restauration.

- [ ] Une archive existe dans `~/backups/`
- [ ] Après suppression puis restauration, les données sont revenues
- [ ] La restauration demande une confirmation avant d'écraser

## Partie 4 — Documentation (25 min)

Votre lecteur est un collègue qui reprend le projet dans six mois, ou vous-même.

1. **README** : démarrer le projet (local), commandes utiles, lien vers le reste. Ajoutez un **schéma d'architecture** en Mermaid (un bloc ```` ```mermaid ````, rendu par GitLab) : développeur, GitLab, runner, registre, votre Docker, reverse proxy, MongoDB.
2. **`docs/runbook.md`** : déployer (staging, production), vérifier l'état, par où commencer en cas d'incident, rollback, sauvegarde/restauration.
3. **`docs/adr/`** : au moins un ADR (*Architecture Decision Record*) : contexte, décision, raisons, conséquences. Par exemple « pourquoi GitLab CI », ou « pourquoi un VPS avec Compose et pas Kubernetes ».

- [ ] Un inconnu peut démarrer l'application avec le seul README
- [ ] Le runbook donne des commandes copiables, pas des généralités
- [ ] L'ADR explique **pourquoi**, pas seulement quoi

## Partie 5 — Vérification finale (5 min)

- [ ] Staging : automatique à chaque fusion sur `main`
- [ ] Production : manuelle, depuis un tag, avec rollback démontré
- [ ] Aucun secret dans le dépôt (Betterleaks vert)
- [ ] La documentation est à jour

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| le job de production n'apparaît pas | le pipeline n'est pas celui d'un **tag** | créez le tag (**Code → Tags**) et ouvrez *son* pipeline |
| le job manuel reste « manual » | il attend votre clic | cliquez sur ▶ ; vérifiez vos droits sur l'environnement protégé |
| `IMAGE_TAG est vide` au rollback | `ROLLBACK_TAG` non renseigné | « Run job » en ajoutant la variable |
| `manifest unknown` au rollback | le tag n'existe pas dans le registre | listez les tags publiés |
| `mongodump: command not found` | l'outil n'est pas installé sur le serveur | lancez-le **dans l'image** `mongo:8.0` (`docker run --rm mongo:8.0 mongodump …`) |
| la restauration remplace les données | `--drop` : comportement voulu | sauvegardez d'abord, confirmez avant d'exécuter |
| le diagramme Mermaid ne s'affiche pas | erreur de syntaxe | testez dans l'aperçu GitLab ; une erreur s'affiche sous le bloc |
| le runbook « marche chez moi » | commandes non testées | exécutez chaque commande sur le serveur avant de l'écrire |

## Défis bonus
1. Automatisez la sauvegarde avec une tâche `cron` sur le serveur. Quels risques (disque, droits) ?
2. Un déploiement « sans interruption » est-il possible avec un simple `docker compose up -d` ? Que faudrait-il changer ?

---

# CORRIGÉS

Branche de référence : `s8-prod`. Fichiers : `.gitlab-ci.yml` (jobs `deploy-production`, `rollback-production`), `scripts/backup-mongo.sh`, `scripts/restore-mongo.sh`, `docs/runbook.md`, `docs/deploiement.md`, `docs/adr/`.

**Partie 1**
```yaml
deploy-production:
  <<: *deploy
  rules:
    - if: $CI_COMMIT_TAG
      when: manual
  variables:
    ENV_NAME: production
    IMAGE_TAG: $CI_COMMIT_TAG
    APP_PORT: $APP_PORT_PROD
    MONGODB_URI: $MONGODB_URI_PROD
    DEPLOY_URL: https://$DEPLOY_USER.$DEPLOY_DOMAIN
  environment:
    name: production
    url: https://$DEPLOY_USER.$DEPLOY_DOMAIN
```
Première ligne du script du modèle : `test -n "$IMAGE_TAG" || { echo "IMAGE_TAG est vide." >&2; exit 1; }`.

**Partie 3** : une sauvegarde est vérifiée quand on l'a **restaurée** au moins une fois ; un fichier d'archive non testé n'est qu'une supposition. Le script de la branche a été exécuté sur le serveur : sauvegarde de 3 documents, suppression, restauration des 3 documents.

**Bonus 2** : `up -d` recrée les conteneurs : coupure brève. Pour l'éviter, démarrer la nouvelle version à côté, vérifier sa santé, puis basculer le proxy (blue-green) ; cela demande plus de ports et un changement de route.
