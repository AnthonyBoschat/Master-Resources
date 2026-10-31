# Séance 4 — Construire et publier l'image Docker en CI

**Durée : 1 h 30.** **Livrable :** les images `api` et `web` publiées dans le Container Registry de votre fork, avec des tags immuables.

## Notions de la séance

- **Image et conteneur** : une *image* est un modèle immuable (système de fichiers + commande) ; un *conteneur* est une image en cours d'exécution. On construit une image une fois, on la lance partout.
- **Registre** : le stockage des images. Le Container Registry de GitLab est intégré au projet ; l'adresse de vos images est `CI_REGISTRY_IMAGE` (suivie de `/api` ou `/web`).
- **Authentification sans mot de passe en dur** : à chaque job, GitLab fournit `CI_REGISTRY_USER` et `CI_REGISTRY_PASSWORD`, un **jeton de job** valable seulement pendant le job. Rien n'est écrit dans le dépôt.
- **Docker dans un job (dind)** : le job utilise le client `docker` et se connecte à un Docker éphémère lancé comme service (`docker:27-dind`). Sans ce service, il n'y a pas de démon pour construire. Notre runner tourne dans un Docker sans privilèges, confiné à son propre compte : un job n'a pas accès au reste du serveur.
- **Tags** : le tag désigne une version d'image. Le *SHA court* du commit donne la traçabilité exacte ; un tag *SemVer* (`v1.2.0`) est lisible par les humains ; `latest` est ambigu et à proscrire en production. Un tag peut être déplacé ; un *digest* (`sha256:…`) ne le peut pas (voir séance 6).
- **Cache de couches** : chaque instruction du Dockerfile produit une couche réutilisée si rien n'a changé. Copier `package*.json` **avant** le reste du code évite de réinstaller les dépendances à chaque modification.
- **Multi-stage** : une première étape construit (Node, outils), la seconde ne garde que le résultat (nginx + fichiers statiques). L'image finale est plus petite et expose moins de surface d'attaque.
- **`rules` et publication** : on ne publie que depuis la branche principale et les tags ; une image de branche ne sera jamais déployée.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 1 | 10 min | Explorer le Container Registry |
| 2 | 10 min | Une variable CI/CD pour le build du front |
| 3 | 25 min | Le job `build-image-api` |
| 4 | 15 min | Le job `build-image-web` |
| 5 | 15 min | Les tags : SHA court et SemVer |
| 6 | 15 min | Récupérer et lancer l'image publiée |

Créez une branche `s4-images` à partir de `main` (qui contient la séance 3).

---

## Partie 1 — Le Container Registry (10 min)

Dans votre fork, ouvrez **Deploy → Container Registry** : il est vide. GitLab fournit à chaque job des variables : `CI_REGISTRY` (adresse), `CI_REGISTRY_IMAGE` (nom de base de vos images), `CI_REGISTRY_USER` et `CI_REGISTRY_PASSWORD` (un **jeton de job**, valable seulement pendant le job).

- [ ] Vous savez retrouver l'adresse de votre registre
- [ ] Vous savez expliquer pourquoi aucun mot de passe n'est écrit dans le dépôt

## Partie 2 — Une variable pour le build du front (10 min)

Pour l'instant, le front reçoit la clé TMDB au moment du build (`ARG VITE_TMDB_API_KEY` dans `docker/Dockerfile`). Créez dans **Settings → CI/CD → Variables** la variable `VITE_TMDB_API_KEY` (valeur : la clé fournie en cours) en cochant **Mask variable**.

- [ ] La variable existe et est masquée (vous ne la voyez plus dans les journaux)

> Gardez ce détail en tête : une clé qui part dans le navigateur n'est pas un secret. On y revient en séance 5.

## Partie 3 — Le job `build-image-api` (25 min)

Ajoutez un stage `package` après `test`, puis un job `build-image-api` :

1. Image `docker:27`, avec le service `docker:27-dind` (un Docker éphémère pour construire).
2. Variables : `DOCKER_TLS_CERTDIR: ""` et `DOCKER_BUILDKIT: "1"`.
3. Connexion : `echo "$CI_REGISTRY_PASSWORD" | docker login -u "$CI_REGISTRY_USER" --password-stdin "$CI_REGISTRY"`.
4. Construction : `docker build -f docker/Dockerfile.api -t "$CI_REGISTRY_IMAGE/api:$CI_COMMIT_SHORT_SHA" .`
5. Publication : `docker push` de ce tag.
6. Restreignez le job aux pipelines de la branche principale et des tags (`rules`).

Poussez sur `main` (par MR) et suivez le job.

- [ ] Le job est vert et l'image `api` apparaît dans le registre
- [ ] Le tag est le SHA court du commit

## Partie 4 — Le job `build-image-web` (15 min)

Même principe pour `docker/Dockerfile`, avec `--build-arg VITE_TMDB_API_KEY="$VITE_TMDB_API_KEY"`. Évitez de dupliquer : utilisez une ancre YAML (`&build_image` / `<<: *build_image`) pour les parties communes.

- [ ] Le registre contient maintenant `api` et `web`
- [ ] Le fichier `.gitlab-ci.yml` ne répète pas les blocs communs

## Partie 5 — Les tags (15 min)

1. Gardez le **SHA court** à chaque build : on sait exactement quel commit tourne.
2. Quand le pipeline part d'un **tag git** (ex. `v1.0.0`), publiez aussi l'image avec ce tag : `docker tag` puis `docker push`.
3. Ne publiez **jamais** de tag `latest` pour déployer : on ne saurait plus dire ce qui tourne.
4. Créez le tag `v0.1.0` sur `main` (**Code → Tags**) et vérifiez le registre.

- [ ] Une image porte le tag `v0.1.0`
- [ ] Vous savez expliquer pourquoi `latest` est ambigu en production

## Partie 6 — Utiliser l'image publiée (15 min)

1. Connectez-vous à votre registre avec un jeton personnel (**avatar → Preferences → Access tokens**, droit `read_registry`) : `docker login registry.gitlab.com`.
2. Récupérez et lancez : `docker run --rm -p 8081:80 <votre-image-web>:<tag>`.

- [ ] L'image tirée depuis le registre démarre sur votre machine

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| `Cannot connect to the Docker daemon at tcp://docker:2375` | service `docker:27-dind` absent, ou `DOCKER_TLS_CERTDIR` mal réglé | déclarez le service et `DOCKER_TLS_CERTDIR: ""` |
| `denied: access forbidden` / `unauthorized` au push | `docker login` oublié ou erroné | ajoutez la ligne de login avec `--password-stdin` |
| le job ne se déclenche pas | `rules` limite à `main` et aux tags | fusionnez la MR dans `main`, ou créez un tag |
| `COPY failed: file not found` | un fichier est exclu par `.dockerignore`, ou le contexte est faux | vérifiez `.dockerignore` et le dernier argument `.` de `docker build` |
| la clé n'apparaît pas dans le build du front | variable non définie ou non transmise | `--build-arg VITE_TMDB_API_KEY="$VITE_TMDB_API_KEY"` et variable créée dans le projet |
| la variable « masquée » est refusée | valeur trop courte (< 8 caractères) ou multi-lignes | vérifiez la valeur |
| `manifest unknown` en tirant l'image | mauvais tag, ou pipeline non terminé | listez les tags dans **Deploy → Container Registry** |
| build très long à chaque pipeline | pas de cache de couches | `--cache-from` sur le tag `:cache` (bonus) |

## Défis bonus
1. Ajoutez un cache de couches (`--cache-from` avec un tag `:cache`) et mesurez le gain au second pipeline.
2. Comparez la taille de l'image `web` finale (nginx) avec celle d'une image qui garderait Node : qu'apporte le multi-stage ?

---

# CORRIGÉS

Branche de référence : `s4-registry`.

**Partie 3**
```yaml
.build_image: &build_image
  stage: package
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH
    - if: $CI_COMMIT_TAG
  image: docker:27
  services: [docker:27-dind]
  variables: { DOCKER_TLS_CERTDIR: "", DOCKER_BUILDKIT: "1" }
  dependencies: []

build-image-api:
  <<: *build_image
  script:
    - echo "$CI_REGISTRY_PASSWORD" | docker login -u "$CI_REGISTRY_USER" --password-stdin "$CI_REGISTRY"
    - docker build -f docker/Dockerfile.api -t "$CI_REGISTRY_IMAGE/api:$CI_COMMIT_SHORT_SHA" .
    - docker push "$CI_REGISTRY_IMAGE/api:$CI_COMMIT_SHORT_SHA"
```

**Partie 5** : dans le script, ajouter
```yaml
    - |
      if [ -n "$CI_COMMIT_TAG" ]; then
        docker tag "$CI_REGISTRY_IMAGE/api:$CI_COMMIT_SHORT_SHA" "$CI_REGISTRY_IMAGE/api:$CI_COMMIT_TAG"
        docker push "$CI_REGISTRY_IMAGE/api:$CI_COMMIT_TAG"
      fi
```

**Bonus** : le tag `:cache` est mutable et réservé au build ; il ne sert jamais au déploiement. Le multi-stage laisse Node et `node_modules` dans l'étape de build et n'embarque que les fichiers statiques dans nginx : image plus petite, moins de surface d'attaque.
