# Séance 7 — Déployer en staging sur le VPS

**Durée : 1 h 30.** **Livrable :** à chaque fusion sur `main`, votre application est déployée automatiquement sur `https://<identifiant>-staging.kptn.fr`.

## Ce que l'enseignant vous a remis
- votre **identifiant** (aussi votre nom de compte sur le VPS) ;
- deux **ports** locaux : staging et production ;
- l'**URI** de vos deux bases MongoDB (staging et production) ;
- l'adresse du serveur, le domaine et l'empreinte du serveur (variables de groupe, déjà héritées par votre fork).

## Notions de la séance

- **Livraison et déploiement** : *livrer* c'est avoir une version prête et validée ; *déployer* c'est la mettre en ligne. Ici, le staging est automatique (recette avant la production) et la production sera manuelle (séance 8).
- **SSH et clés** : une paire de clés (privée/publique). La publique est installée sur le serveur ; la privée prouve votre identité et ne quitte jamais vos mains, sauf dans une variable CI protégée. Une clé **dédiée** au déploiement se révoque sans toucher à votre accès personnel.
- **`known_hosts` et empreinte du serveur** : à la première connexion, SSH mémorise l'empreinte du serveur. La fournir à l'avance (variable de groupe) protège contre un serveur usurpé : sans elle, un attaquant pourrait se faire passer pour le VPS.
- **Variable de type File** : GitLab écrit la valeur dans un fichier temporaire et donne son chemin ; pas besoin d'encoder la clé en base64.
- **Le serveur ne reconstruit jamais** : il télécharge (`pull`) l'image que la pipeline a testée et publiée. Sinon, on déploierait quelque chose que personne n'a testé.
- **Un fichier Compose de déploiement** : sans `build:`, sans base de données locale ; la configuration vient d'un fichier `.env` écrit par le job. `docker compose -p <nom>` isole les piles staging et production sur le même serveur.
- **`127.0.0.1:` devant le port** : le conteneur n'est joignable que depuis le serveur lui-même ; le reverse proxy (géré par l'enseignant) le publie en HTTPS sous `https://<identifiant>-staging.kptn.fr`. Sans ce préfixe, votre application serait exposée directement sur Internet.
- **Votre Docker est le vôtre** : sur le VPS, chaque élève a son propre Docker sans privilèges (*rootless*) : vos conteneurs ne voient pas ceux des autres.
- **Healthcheck** : `depends_on` n'attend que le démarrage ; un `healthcheck` attend qu'un service soit *sain*. Après un déploiement, la pipeline vérifie `/health` **depuis l'extérieur** : un déploiement terminé mais cassé reste un échec.
- **Secrets par l'entrée standard** : une commande distante passée en argument est visible dans la liste des processus ; les données envoyées par `stdin` ne le sont pas.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 1 | 10 min | Une clé SSH dédiée au déploiement |
| 2 | 10 min | Les variables CI/CD |
| 3 | 15 min | Le fichier Compose de déploiement |
| 4 | 15 min | Une sonde de santé : `/health` |
| 5 | 30 min | Le job `deploy-staging` |
| 6 | 10 min | Vérifier, puis piège « image reconstruite » |

Créez une branche `s7-deploy` à partir de `main`.

---

## Partie 1 — Une clé SSH dédiée (10 min)

Une clé de déploiement ne sert qu'à la pipeline : si elle fuite, on la révoque sans toucher à votre accès personnel.

1. Générez une paire : `ssh-keygen -t ed25519 -f deploy_key -N ""`. **Ne committez jamais ce fichier** (ajoutez `deploy_key*` au `.gitignore` avant tout).
2. Transmettez la clé **publique** (`deploy_key.pub`) à l'enseignant, qui l'installe sur votre compte.
3. Testez : `ssh -i deploy_key <identifiant>@<serveur> docker ps`.

- [ ] La connexion fonctionne avec votre clé
- [ ] `git status` ne montre pas `deploy_key`

## Partie 2 — Les variables CI/CD (10 min)

Dans **Settings → CI/CD → Variables** de votre fork :

| Variable | Type | Options |
|---|---|---|
| `DEPLOY_USER` | Variable | votre identifiant |
| `DEPLOY_SSH_KEY` | **File** | contenu de la clé **privée** (`deploy_key`) |
| `APP_PORT_STAGING` | Variable | votre port staging |
| `MONGODB_URI_STAGING` | Variable | **Masquée**, et **décochez « Expand variable reference »** |

> L'URI Mongo contient des caractères spéciaux (`?`, `=`…) : GitLab n'accepte de la masquer que si l'expansion des références (`$…`) est désactivée.
>
> Le type **File** évite d'encoder la clé : GitLab l'écrit dans un fichier temporaire et fournit le chemin.

- [ ] Les quatre variables existent ; l'URI Mongo est masquée
- [ ] Vous savez dire quelles variables viennent du groupe

## Partie 3 — Le fichier Compose de déploiement (15 min)

En staging et en production, le serveur **ne reconstruit pas** : il télécharge l'image déjà testée. Créez `docker-compose.deploy.yml` avec deux services :
- `api` : `image: ${CI_REGISTRY_IMAGE}/api:${IMAGE_TAG}`, `restart: unless-stopped`, variables `MONGODB_URI` et `TMDB_API_KEY` ;
- `app` : `image: ${CI_REGISTRY_IMAGE}/web:${IMAGE_TAG}`, `restart: unless-stopped`, port publié **`127.0.0.1:${APP_PORT}:80`**.

Pourquoi `127.0.0.1:` ? Le reverse proxy du serveur (Traefik) est sur la même machine : il relaie vers ce port local. Sans `127.0.0.1`, votre application serait exposée directement sur Internet.

- [ ] Le fichier ne contient ni `build:` ni service `db`
- [ ] Vous savez expliquer le rôle du préfixe `127.0.0.1`

## Partie 4 — Une sonde de santé (15 min)

1. Dans `api/app.js`, ajoutez `GET /health` : il interroge Mongo (`getDb().command({ ping: 1 })`) ; réponse `{"status":"ok"}`, ou **503** `{"status":"ko", "error": …}` si Mongo ne répond pas.
2. Dans `docker/nginx.conf`, ajoutez `location /health { proxy_pass http://api:3000/health; }`.
3. Ajoutez deux tests unitaires (base saine, base en panne) et un test d'intégration.
4. Ajoutez un `healthcheck:` aux deux services de `docker-compose.deploy.yml` (par exemple `wget -qO- http://localhost:3000/health`) ; `app` attend que `api` soit `healthy` (`depends_on` avec `condition: service_healthy`). Pour nginx, utilisez `127.0.0.1` plutôt que `localhost`.

- [ ] `/health` répond `ok` en local, `ko` (503) quand Mongo est arrêté
- [ ] Les tests passent

## Partie 5 — Le job `deploy-staging` (30 min)

Ajoutez un stage `deploy`, puis un job qui, **à chaque pipeline de la branche principale** :
1. s'installe : image `alpine:3`, `apk add --no-cache openssh-client curl`, écrit `DEPLOY_SSH_KNOWN_HOSTS` dans `~/.ssh/known_hosts`, `chmod 600 "$DEPLOY_SSH_KEY"` ;
2. crée le dossier `~/moviejs-staging` sur le serveur et y copie `docker-compose.deploy.yml` (renommé `docker-compose.yml`) avec `scp` ;
3. écrit un fichier `.env` distant avec `CI_REGISTRY_IMAGE`, `IMAGE_TAG` (le SHA court), `MONGODB_URI`, `TMDB_API_KEY`, `APP_PORT`. **Les secrets transitent par l'entrée standard** (`printf … | ssh … 'cat > .env'`), jamais dans la ligne de commande distante ;
4. se connecte au registre sur le serveur (`docker login --password-stdin`), puis `docker compose -p moviejs-staging pull` et `up -d` ;
5. vérifie `https://<identifiant>-staging.kptn.fr/health` depuis l'extérieur (jusqu'à 20 essais espacés de 5 s) et **échoue** sinon.

Déclarez aussi l'environnement GitLab `staging` avec son adresse.

- [ ] Le job est vert après une fusion sur `main`
- [ ] Le journal contient « Healthcheck OK »
- [ ] **Aucun secret** n'apparaît dans le journal

## Partie 6 — Vérifier et comprendre (10 min)

1. Ouvrez `https://<identifiant>-staging.kptn.fr` : votre application doit répondre en HTTPS.
2. **Piège** : quelqu'un modifie le code sur `main` pendant que le staging tourne. Si le serveur reconstruisait l'image au lieu de la télécharger, qu'est-ce qui serait déployé ? Est-ce ce que la pipeline a testé ?

- [ ] Votre application est visible sur Internet
- [ ] Vous savez expliquer « l'image reconstruite n'est pas l'artefact testé »

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| `Permission denied (publickey)` | clé publique non installée, mauvais fichier, ou droits trop larges | `chmod 600 "$DEPLOY_SSH_KEY"` ; vérifiez avec l'enseignant que votre clé publique est installée |
| `Host key verification failed` | `DEPLOY_SSH_KNOWN_HOSTS` vide ou erroné | variable de groupe, ligne de `ssh-keyscan -t ed25519` |
| `pull access denied` | `docker login` oublié ou nom d'image faux | vérifiez `CI_REGISTRY_IMAGE` et le `docker login` |
| `port is already allocated` | mauvais `APP_PORT`, ou ancienne pile | utilisez **votre** port ; `docker compose -p moviejs-staging down` |
| `variable is not set` (avertissement Compose) | `.env` absent ou incomplet | vérifiez le `printf` du job |
| Healthcheck KO, HTTP 502 | conteneur arrêté, non sain, ou mauvais port | `docker compose -p moviejs-staging ps` et `logs api` sur le serveur |
| HTTP 404 « page not found » (en-têtes Traefik) | mauvais nom d'hôte | l'adresse exacte est `<identifiant>-staging.kptn.fr` |
| la variable est refusée par GitLab | URI Mongo avec caractères spéciaux | masquée **et** « Expand variable reference » décoché |
| `/health` répond `ko` | API sans accès à MongoDB | vérifiez `MONGODB_URI` (staging) |

## Défis bonus
1. Arrêtez volontairement le conteneur `api` sur le serveur : que répond `/health` ? Que fait le prochain déploiement ?
2. Ajoutez au journal du job l'adresse et le tag déployés.

---

# CORRIGÉS

Branche de référence : `s7-deploy`. Fichiers : `docker-compose.deploy.yml`, bloc `deploy-staging` de `.gitlab-ci.yml`.

**Partie 5 — squelette du job**
```yaml
deploy-staging:
  stage: deploy
  image: alpine:3
  dependencies: []
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH
  variables:
    ENV_NAME: staging
    IMAGE_TAG: $CI_COMMIT_SHORT_SHA
    APP_PORT: $APP_PORT_STAGING
    MONGODB_URI: $MONGODB_URI_STAGING
    DEPLOY_URL: https://$DEPLOY_USER-staging.$DEPLOY_DOMAIN
  environment:
    name: staging
    url: https://$DEPLOY_USER-staging.$DEPLOY_DOMAIN
  before_script:
    - apk add --no-cache openssh-client curl
    - mkdir -p ~/.ssh && echo "$DEPLOY_SSH_KNOWN_HOSTS" > ~/.ssh/known_hosts
    - chmod 600 "$DEPLOY_SSH_KEY"
    - export SSH="ssh -i $DEPLOY_SSH_KEY -o IdentitiesOnly=yes $DEPLOY_USER@$DEPLOY_HOST"
  script:
    - $SSH "mkdir -p ~/moviejs-$ENV_NAME"
    - scp -i "$DEPLOY_SSH_KEY" -o IdentitiesOnly=yes docker-compose.deploy.yml "$DEPLOY_USER@$DEPLOY_HOST:moviejs-$ENV_NAME/docker-compose.yml"
    - |
      printf 'CI_REGISTRY_IMAGE=%s\nIMAGE_TAG=%s\nMONGODB_URI=%s\nTMDB_API_KEY=%s\nAPP_PORT=%s\n' \
        "$CI_REGISTRY_IMAGE" "$IMAGE_TAG" "$MONGODB_URI" "$TMDB_API_KEY" "$APP_PORT" \
        | $SSH "umask 077; cat > moviejs-$ENV_NAME/.env"
    - echo "$CI_REGISTRY_PASSWORD" | $SSH "docker login -u '$CI_REGISTRY_USER' --password-stdin '$CI_REGISTRY'"
    - $SSH "cd moviejs-$ENV_NAME && docker compose -p moviejs-$ENV_NAME pull && docker compose -p moviejs-$ENV_NAME up -d"
    - |
      for i in $(seq 1 20); do
        curl -fsS "$DEPLOY_URL/health" | grep -q '"status":"ok"' && { echo "Healthcheck OK : $DEPLOY_URL"; exit 0; }
        sleep 5
      done
      exit 1
```
La version complète de la branche utilise une ancre `.deploy` partagée avec la production (séance 8).

**Partie 6** : le serveur ne contient jamais le code source ; il ne fait que `pull`. Reconstruire depuis un code différent déploierait quelque chose que la pipeline n'a jamais testé.
