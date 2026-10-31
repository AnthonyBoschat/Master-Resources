# Séance 1 — Mettre en place votre atelier GitLab

**Durée : 1 h 30** (travaux pratiques, après la partie théorique).
**Livrable :** votre fork du projet dans le groupe du cours, qui tourne en local, avec un premier pipeline vert.

## Notions de la séance

- **CI/CD en bref** : l'*intégration continue* valide automatiquement chaque modification (construire, vérifier, tester) ; la *livraison continue* garde le résultat prêt à être déployé ; le *déploiement continu* le met en production sans intervention. Vous construirez ces trois étages au fil des séances.
- **Fork, clone, branche** : un *fork* est une copie du projet côté GitLab, à vous ; un *clone* est une copie sur votre machine ; une *branche* est une ligne de travail à l'intérieur d'un même dépôt.
- **Pourquoi un fork par élève** : vous avez vos propres variables CI/CD, votre propre branche `main` protégée et votre propre historique. Le *runner* du groupe, lui, sert tous les forks.
- **Pipeline, stage, job, runner** : un *pipeline* se déclenche à chaque push ; il est fait de *stages* exécutés l'un après l'autre ; chaque stage contient des *jobs* qui tournent en parallèle ; un *runner* est la machine qui exécute les jobs.
- **Le fichier `.env`** : il contient des valeurs propres à votre machine (ici une clé d'API). Il figure dans `.gitignore` : on ne committe jamais un secret.
- **Les trois conteneurs** : `db` (MongoDB) stocke les films ; `api` (Node) les expose en JSON ; `app` (nginx) sert le front et relaie les appels `/api/` vers `api`.
- **Variables prédéfinies** : GitLab fournit des variables à chaque job (`GITLAB_USER_LOGIN`, `CI_COMMIT_SHORT_SHA`…) : vous en utiliserez beaucoup.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 0 | 5 min | Vérifier vos prérequis |
| 1 | 20 min | Rejoindre le groupe et forker le projet |
| 2 | 30 min | Cloner et lancer l'application en local |
| 3 | 15 min | Travailler sur une branche |
| 4 | 20 min | Votre premier pipeline vert |

---

## Partie 0 — Prérequis (5 min)

- [ ] Un compte GitLab, et l'invitation au groupe du cours acceptée
- [ ] `git --version` fonctionne
- [ ] `docker --version` et `docker compose version` fonctionnent
- [ ] `node --version` affiche une version 24 ou plus
- [ ] La clé TMDB fournie en cours (à garder pour vous, ne la collez dans aucun fichier du dépôt)

## Partie 1 — Rejoindre le groupe et forker (20 min)

Un **fork** est votre copie personnelle du projet de référence. Elle a ses propres branches, ses propres variables CI/CD et son propre historique.

1. Ouvrez le projet **cicd2026** dans le groupe du cours.
2. Cliquez sur **Fork**.
3. Choisissez comme espace de noms le **groupe du cours**, nommez le projet avec votre prénom (en minuscules, sans espace), laissez la visibilité **Privée**, et dans « Branches à inclure » choisissez **Uniquement la branche par défaut**.
4. Dans votre fork, ouvrez **Settings → CI/CD → Runners** et repérez le runner du groupe (`vps-ovh-cours`).
5. Ajoutez votre clé SSH à GitLab (**avatar → Preferences → SSH Keys**) si ce n'est pas déjà fait.

**Critères de réussite**
- [ ] Votre fork apparaît dans le groupe, sous votre prénom
- [ ] La visibilité est « Privée »
- [ ] Vous voyez le runner du groupe actif dans votre fork

## Partie 2 — Cloner et lancer en local (30 min)

1. Clonez **votre fork** (et non le projet de référence) avec l'adresse SSH proposée par le bouton **Code**.
2. Copiez le fichier d'exemple et renseignez la clé :
   ```bash
   cp .env.example .env
   ```
   Ouvrez `.env` et complétez `VITE_TMDB_API_KEY=`.
3. Démarrez l'application :
   ```bash
   docker compose up --build
   ```
4. Ouvrez <http://localhost:8080>. Vérifiez aussi <http://localhost:8080/api/movies>.
5. Arrêtez l'application avec `docker compose down`.
6. Lancez `git status` : le fichier `.env` ne doit **pas** apparaître.

**Critères de réussite**
- [ ] La page affiche des fiches de films
- [ ] `/api/movies` renvoie une liste JSON de 48 films
- [ ] `git status` n'affiche pas `.env` (il est ignoré par `.gitignore`)
- [ ] Vous savez dire quel conteneur fait quoi : `db`, `api`, `app`

> Question : pourquoi `docker compose down -v` efface-t-il les données alors que `docker compose down` les conserve ?

## Partie 3 — Travailler sur une branche (15 min)

Nous suivons une stratégie de branches courtes fusionnées par Merge Request. On ne pousse jamais directement sur `main`.

1. Créez votre branche de travail : `git switch -c s1-atelier`
2. Dans **Settings → Repository → Protected branches** de votre fork, vérifiez que `main` est protégée. Si vous ne pouvez pas modifier ce réglage, prévenez l'enseignant.

**Critères de réussite**
- [ ] Vous êtes sur la branche `s1-atelier` (`git branch --show-current`)
- [ ] `main` est protégée dans votre fork

## Partie 4 — Votre premier pipeline vert (20 min)

1. À la racine du dépôt, créez un fichier `.gitlab-ci.yml` avec un seul job :
   ```yaml
   bonjour:
     image: node:24-alpine
     script:
       - node --version
       - echo "Pipeline de $GITLAB_USER_LOGIN"
   ```
2. Validez et poussez la branche :
   ```bash
   git add .gitlab-ci.yml
   git commit -m "Premier pipeline"
   git push -u origin s1-atelier
   ```
3. Ouvrez **Build → Pipelines**, cliquez sur le job `bonjour` et lisez le journal.
4. Repérez dans le journal : l'image utilisée, le nom du runner, la version de Node.

**Critères de réussite**
- [ ] Le pipeline est vert
- [ ] Vous retrouvez le nom du runner dans le journal du job
- [ ] Vous savez expliquer la différence entre un *pipeline*, un *stage* et un *job*

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| `Permission denied (publickey)` au clone | clé SSH absente de GitLab, ou mauvaise adresse | ajoutez la clé publique dans GitLab ; clonez avec l'adresse SSH de **votre** fork |
| `port is already allocated` (8080) | un autre programme utilise le port | arrêtez-le, ou `docker compose down` puis relancez |
| `docker: unknown command: docker compose` | ancienne version de Docker | installez le plugin Compose v2 |
| `permission denied ... docker.sock` (Linux) | votre compte n'a pas accès à Docker | ajoutez-vous au groupe `docker` puis ouvrez une nouvelle session |
| la page s'affiche sans film | l'API n'est pas démarrée, ou la base est vide | `docker compose logs api` ; au premier démarrage l'API charge `public/movies.json` |
| pipeline « pending » (bloqué) | aucun runner disponible | vérifiez le runner du groupe dans **Settings → CI/CD → Runners** ; un job sans tag doit pouvoir s'y exécuter |
| `git push` refusé sur `main` | `main` est protégée | travaillez sur une branche, puis ouvrez une Merge Request |
| `.env` apparaît dans `git status` | `.gitignore` modifié ou fichier déjà suivi | ne le committez pas ; `git rm --cached .env` s'il a été ajouté |

## Défis bonus

1. Provoquez volontairement un échec (une commande qui n'existe pas) et lisez le message. Corrigez et repassez au vert.
2. Calculez le **délai de livraison** de votre commit : du moment du `git push` à la fin du pipeline. Quelle métrique DORA est-ce ?

---

# CORRIGÉS

## Partie 1
- Fork : **Fork → espace de noms = groupe du cours**, nom en minuscules, visibilité Privée.
- Le runner du groupe est visible, hérité du groupe, dans **Settings → CI/CD → Runners**.

## Partie 2
```bash
git clone git@gitlab.com:<groupe>/<prenom>.git
cd <prenom>
cp .env.example .env     # puis renseigner VITE_TMDB_API_KEY
docker compose up --build
```
- `db` : MongoDB stocke les films. `api` : Node/Express les expose en JSON. `app` : nginx sert le front et relaie `/api/` vers `api`.
- `docker compose down` supprime les conteneurs mais garde le volume nommé `mongo_data` ; `-v` supprime aussi ce volume, donc les données.

## Partie 3
```bash
git switch -c s1-atelier
git branch --show-current
```

## Partie 4
Le fichier proposé ci-dessus convient. Le journal contient l'image `node:24-alpine`, la ligne `Running with gitlab-runner ... (vps-ovh-cours)` et `v24.x.x`.
- Pipeline : tout ce qui se déclenche sur un push. Stage : une étape qui contient des jobs. Job : une tâche exécutée par un runner.

## Bonus
1. Une commande inconnue donne `sh: ...: not found` et un job rouge. Corriger le script puis pousser relance le pipeline.
2. C'est le **délai de livraison** (*lead time*), ici jusqu'à la fin du pipeline, pas jusqu'à la production.
