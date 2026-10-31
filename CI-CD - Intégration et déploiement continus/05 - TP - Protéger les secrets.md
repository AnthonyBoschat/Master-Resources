# Séance 5 — Protéger les secrets : proxy TMDB, Betterleaks, Semgrep

**Durée : 1 h 30.** **Livrable :** plus aucune clé dans le navigateur ni dans le dépôt, et deux contrôles de sécurité bloquants en CI.

## Notions de la séance

- **Qu'est-ce qu'un secret** : une valeur qui donne un accès (clé d'API, mot de passe, jeton, clé privée). Elle ne doit apparaître ni dans le code, ni dans une image, ni dans un journal.
- **Le client n'est jamais sûr** : tout ce que reçoit le navigateur (HTML, JavaScript, requêtes) est lisible par l'utilisateur. Une clé embarquée dans le front est donc publique. Le remède est un **proxy côté serveur** : le navigateur appelle *notre* API, qui appelle TMDB avec la clé, conservée dans une variable d'environnement du serveur.
- **Variables CI/CD** : *masquée* (cachée dans les journaux), *protégée* (fournie seulement aux branches et tags protégés), de type *File* (écrite dans un fichier temporaire), définie au niveau du *projet* ou du *groupe*. Aucune ne dispense de ne **jamais** committer un secret.
- **Un secret qui a fuité est brûlé** : le supprimer du code ne suffit pas, il reste dans l'historique git. On le **révoque** d'abord (immédiat), on nettoie ensuite si nécessaire.
- **Quatre types d'analyse** : *secrets* (des clés dans le dépôt, Betterleaks), *SAST* (motifs dangereux dans le code source, Semgrep), *SCA* (failles des dépendances, Trivy — séance 6), *image* (failles du système et des paquets de l'image, Trivy — séance 6).
- **Betterleaks** : parcourt tous les commits et applique des règles (formats de clés connus, entropie élevée). Un **faux positif** se traite par un fichier d'exclusion (`.betterleaksignore` ou `.gitleaksignore`, avec l'empreinte de la découverte), jamais en désactivant le scan.
- **Un contrôle de sécurité doit bloquer** : un job qui passe toujours (ou dont l'échec est toléré) ne protège de rien.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 1 | 10 min | Constater la fuite |
| 2 | 25 min | Le proxy TMDB côté API |
| 3 | 15 min | Retirer la clé du front et des images |
| 4 | 10 min | Les secrets dans GitLab |
| 5 | 20 min | Betterleaks : chercher les secrets dans l'historique |
| 6 | 10 min | Semgrep : analyser le code |

Créez une branche `s5-secrets` à partir de `main`. Avant de commencer, posez un tag `avant-fix-tmdb` sur `main` : il garde l'état « naïf » pour la comparaison.

---

## Partie 1 — Constater la fuite (10 min)

Lancez l'application (`docker compose up --build`), ouvrez <http://localhost:8080>, puis les outils de développement du navigateur.

1. Onglet **Réseau** : cliquez sur « Détails TMDB » d'une fiche. Retrouvez l'adresse appelée : la clé `api_key=` y figure en clair.
2. Onglet **Sources** (ou dans l'image) : cherchez la clé dans le fichier JavaScript compilé.

- [ ] Vous avez retrouvé la clé dans une requête
- [ ] Vous avez retrouvé la clé dans le JavaScript servi au navigateur

> Tout ce qui part vers le navigateur est public. Une clé côté client est une clé fuitée.

## Partie 2 — Le proxy côté API (25 min)

Le navigateur appellera **notre** API ; c'est elle qui appelle TMDB et garde la clé.

1. Dans `api/app.js`, ajoutez à `createApp` les paramètres `tmdbApiKey` et `fetchImpl` (par défaut `fetch`), et une route `GET /api/tmdb/search?title=` :
   - sans paramètre `title` : réponse **400** ;
   - sinon : appel à `https://api.themoviedb.org/3/search/movie?api_key=…&query=…` (le titre doit être encodé avec `encodeURIComponent`) et renvoi du JSON.
2. Dans `api/server.js`, passez `tmdbApiKey: process.env.TMDB_API_KEY`.
3. Dans `docker-compose.yml`, donnez à l'API la variable `TMDB_API_KEY: ${TMDB_API_KEY}`.
4. Dans `.env` et `.env.example`, la variable s'appelle désormais `TMDB_API_KEY`.
5. Écrivez deux tests unitaires de la route : 400 sans titre ; avec un titre, TMDB est appelé avec la clé et **la clé ne figure pas dans la réponse** (injectez un faux `fetchImpl`).

- [ ] `npm run test:unit` passe, avec les deux nouveaux tests
- [ ] La route renvoie bien 400 sans paramètre

## Partie 3 — Retirer la clé du front et des images (15 min)

1. Dans `src/fetch.js`, `fetchMovieDetailsFromTMDB(title)` appelle `/api/tmdb/search?title=…` (l'adresse relative passe par nginx) : plus de clé, plus de `import.meta.env`.
2. Dans `docker/Dockerfile`, supprimez les lignes `ARG VITE_TMDB_API_KEY` et `ENV VITE_TMDB_API_KEY=…`.
3. Dans `docker-compose.yml` et dans `.gitlab-ci.yml` (job `build-image-web`), supprimez le passage de `VITE_TMDB_API_KEY`.
4. `seedMovies.js` lit désormais `process.env.TMDB_API_KEY`.
5. Reconstruisez et recommencez la partie 1 : la clé doit avoir disparu de la requête **et** du JavaScript.

- [ ] Plus aucune occurrence de la clé dans les requêtes du navigateur
- [ ] `grep -r VITE_TMDB .` (hors `node_modules`) ne renvoie rien
- [ ] Le bouton « Détails TMDB » fonctionne toujours (réponse dans la console)

## Partie 4 — Les secrets dans GitLab (10 min)

1. Supprimez la variable `VITE_TMDB_API_KEY` de votre projet (elle n'a plus de raison d'être).
2. Retenez les quatre réglages d'une variable CI/CD : **masquée** (cachée dans les journaux), **protégée** (réservée aux branches et tags protégés), **jamais committée**, **renouvelable** (on peut la révoquer sans toucher au code).
3. La clé TMDB du cours est une variable **de groupe** gérée par l'enseignant : votre fork l'hérite.

- [ ] Vous savez dire où vit chaque secret du projet

## Partie 5 — Betterleaks : chercher les secrets dans l'historique (20 min)

Un secret supprimé d'un fichier reste dans l'historique git. Betterleaks parcourt **tous** les commits.

1. Ajoutez un job `betterleaks` au stage `test` : image `ghcr.io/betterleaks/betterleaks:v1.9.0` (avec `entrypoint: [""]`), variable `GIT_DEPTH: "0"` (clone complet), script `betterleaks git . --no-banner --redact`.
2. **Piège** : git refuse parfois un dépôt dont le propriétaire diffère de l'utilisateur (« dubious ownership »). L'outil ne lit alors **rien** et affiche quand même « no leaks found ». Corrigez avec les variables `GIT_CONFIG_COUNT=1`, `GIT_CONFIG_KEY_0=safe.directory`, `GIT_CONFIG_VALUE_0=$CI_PROJECT_DIR`, et faites échouer le job si la sortie ne contient pas `scanned ~` suivi d'un nombre non nul.
3. **Essai** : sur une branche jetable, commitez un fichier contenant `const apiKey = '<32 caractères hexadécimaux au hasard>'`. Le job doit échouer.
4. Que faire d'un secret qui a déjà fuité ? Réfléchissez : supprimer la ligne suffit-il ?

- [ ] Le job est vert sur votre branche
- [ ] Le job devient rouge quand un secret est commité
- [ ] Vous savez expliquer le piège du scan à vide

## Partie 6 — Semgrep : analyser le code (10 min)

1. Ajoutez un job `semgrep` (stage `test`) : image `semgrep/semgrep:1.182.1`, script `semgrep scan --config p/javascript --metrics=off --error api src seedMovies.js`.
2. Vérifiez qu'il est vert. Que signifie `--error` ?

- [ ] Le job `semgrep` est vert
- [ ] Vous savez dire la différence entre un scan de secrets, d'images et de code (SAST)

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| Betterleaks affiche `no leaks found` mais `scanned ~0 bytes` | git refuse le dossier (« dubious ownership ») | variables `GIT_CONFIG_*` (`safe.directory`) et vérification de la sortie |
| Betterleaks ne voit qu'un commit | clone superficiel | `GIT_DEPTH: "0"` |
| un secret est détecté dans un fichier de test | donnée factice qui ressemble à une clé | remplacez-la par une valeur évidente, ou ajoutez l'empreinte à `.betterleaksignore` |
| `semgrep` : `Failed to download config` | pas d'accès réseau au registre de règles | vérifiez la sortie réseau du runner |
| Semgrep signale `eval` ou une injection | motif dangereux réel | corrigez le code ; ne supprimez pas la règle |
| le bouton « Détails TMDB » ne renvoie rien après le proxy | `TMDB_API_KEY` absente de l'environnement de l'API | `.env` à la racine ; `docker compose up --build` |
| la clé reste visible dans le navigateur | ancienne image en cache | `docker compose build --no-cache app` |
| la route répond 200 avec un objet d'erreur TMDB | clé invalide | vérifiez la clé fournie en cours |

## Défis bonus
1. Ajoutez volontairement un motif dangereux (`eval(req.query.x)`) dans l'API : Semgrep doit le détecter.
2. Que faut-il faire de l'ancienne clé après une fuite ? Dans quel ordre : révoquer, nettoyer l'historique, ou les deux ?

---

# CORRIGÉS

Branche de référence : `s5-secrets` (avec `avant-fix-tmdb` sur `s4-registry`).

**Partie 2 — route**
```js
app.get('/api/tmdb/search', async (req, res) => {
  const { title } = req.query
  if (!title) return res.status(400).json({ error: 'Paramètre "title" requis' })
  const url = `https://api.themoviedb.org/3/search/movie?api_key=${tmdbApiKey}&query=${encodeURIComponent(title)}`
  const response = await fetchImpl(url)
  res.json(await response.json())
})
```

**Partie 5 — job**
```yaml
betterleaks:
  stage: test
  dependencies: []
  image:
    name: ghcr.io/betterleaks/betterleaks:v1.9.0
    entrypoint: [""]
  variables:
    GIT_DEPTH: "0"
    GIT_CONFIG_COUNT: "1"
    GIT_CONFIG_KEY_0: safe.directory
    GIT_CONFIG_VALUE_0: $CI_PROJECT_DIR
  script:
    - |
      betterleaks git . --no-banner --redact > scan.log 2>&1 || { cat scan.log; exit 1; }
      cat scan.log
      grep -Eq "scanned ~[1-9]" scan.log || { echo "Scan à vide"; exit 1; }
```
- Supprimer la ligne ne suffit pas : le secret reste dans l'historique. Il faut d'abord **révoquer** la clé (elle est publique), puis éventuellement réécrire l'historique. Un secret révoqué ne sert plus à rien.
- Bonus 2 : révoquer d'abord (immédiat), puis nettoyer.

**Partie 6** : `--error` fait échouer le job (code de sortie non nul) quand une alerte est trouvée ; sans lui, le job serait vert quoi qu'il arrive.
