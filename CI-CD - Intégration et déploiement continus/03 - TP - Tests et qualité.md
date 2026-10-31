# Séance 3 — Tests et qualité : une pipeline qui protège `main`

**Durée : 1 h 30.** **Livrable :** une Merge Request bloquée tant que les tests et la couverture ne sont pas au vert.

## Notions de la séance

- **La pyramide des tests** : beaucoup de tests *unitaires* (rapides, sans dépendance), moins de tests d'*intégration* (plusieurs composants réels ensemble, par exemple l'API et MongoDB), très peu de tests *end-to-end* (le parcours complet dans un navigateur, lents et fragiles). Plus on monte, plus le test coûte cher.
- **Double de test et vraie dépendance** : un test unitaire remplace la base par un *faux* objet ; un test d'intégration utilise la **vraie** base. On teste ainsi à la fois la logique et la connexion réelle.
- **Pourquoi séparer `app.js` et `server.js`** : `server.js` ouvre une connexion et un port dès qu'on l'importe. `app.js` fabrique l'application sans effet de bord : un test peut la démarrer sur un port libre (`listen(0)`), l'appeler et l'arrêter.
- **Service de job** : en CI, `services:` démarre un conteneur à côté du job (ici MongoDB). Il est joignable par un nom d'hôte, le nom de l'image sans le tag : `mongo`. En local, c'est `localhost` et le port publié.
- **Une base d'intégration se nettoie** : elle garde les données de l'exécution précédente, contrairement à une base en mémoire. On la vide avant et après les tests.
- **Couverture de code** : la proportion de lignes exécutées par les tests. Elle repère le code jamais testé, mais ne prouve pas que les tests vérifient quelque chose. Un **seuil** fait échouer le job quand la couverture descend : on le règle au niveau actuel et on le relève progressivement.
- **Tests instables (*flaky*)** : un test qui réussit ou échoue au hasard (date, ordre, réseau) ruine la confiance : on finit par ignorer la pipeline.
- **Protéger `main`** : exiger une Merge Request et un pipeline vert rend impossible de fusionner du code cassé. C'est le garde-fou du *quality gate*.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 1 | 10 min | Ranger les tests selon la pyramide |
| 2 | 20 min | Un vrai MongoDB pour les tests d'intégration |
| 3 | 25 min | Tester l'API : extraire l'application Express |
| 4 | 15 min | Les jobs de tests en CI |
| 5 | 10 min | Couverture et seuil bloquant |
| 6 | 10 min | Protéger `main` |

Créez une branche `s3-tests` à partir de votre `main`.

---

## Partie 1 — Ranger les tests (10 min)

Les tests actuels sont dans `tests/functional/`. La pyramide distingue trois niveaux : **unitaires** (rapides, sans dépendance), **intégration** (plusieurs composants réels ensemble), **end-to-end** (parcours complet, lents, peu nombreux).

1. Déplacez `tests/functional/image.test.js` vers `tests/unit/`.
2. Déplacez `tests/functional/db.test.js` vers `tests/integration/`.
3. Dans `vitest.config.js`, remplacez le motif `tests/functional/**` par `tests/integration/**`.
4. Dans `package.json`, ajoutez les scripts `test:unit` (`vitest run tests/unit`) et `test:integration` (`vitest run tests/integration`).

- [ ] `npm run test:unit` lance les tests de `tests/unit/` seulement
- [ ] Le dossier `tests/functional/` n'existe plus

## Partie 2 — Un vrai MongoDB pour l'intégration (20 min)

`db.test.js` utilise `mongodb-memory-server`, une base simulée en mémoire. Un test d'intégration doit vérifier la **vraie** connexion, donc un vrai MongoDB.

1. Dans `docker-compose.yml`, publiez le port du service `db` : `"27018:27017"` (27018 évite un conflit avec un `mongod` local).
2. Réécrivez le début de `tests/integration/db.test.js` : la connexion se fait avec `process.env.MONGO_URL` (par défaut `mongodb://localhost:27018/moviejs-test`). Avant les tests, videz la collection `movies` (une vraie base garde les données de l'exécution précédente) ; après, videz-la à nouveau et fermez la connexion.
3. Supprimez la dépendance `mongodb-memory-server` (`npm uninstall mongodb-memory-server`).
4. Lancez `docker compose up -d db`, puis `npm run test:integration`.

- [ ] Les tests d'intégration passent contre le service `db`
- [ ] `mongodb-memory-server` n'apparaît plus dans `package.json`
- [ ] Vous pouvez relancer les tests plusieurs fois de suite sans échec (la base est nettoyée)

## Partie 3 — Tester l'API (25 min)

`api/server.js` démarre le serveur dès qu'on l'importe : impossible de le tester proprement. On sépare **la logique des routes** (testable) du **démarrage** (connexion, seed, port).

1. Créez `api/app.js` qui exporte `createApp({ getDb })`. La fonction crée l'application Express, déclare la route `GET /api/movies` (qui lit `getDb().collection('movies')`, sans le champ `_id`) et retourne l'application.
2. Dans `api/server.js`, supprimez la route et importez `createApp` : `const app = createApp({ getDb: () => db })`.
3. Dans `docker/Dockerfile.api`, copiez aussi `api/app.js` dans l'image.
4. Ajoutez `express` aux dépendances de développement du projet racine (les tests importent `api/app.js`).
5. Écrivez deux tests :
   - **unitaire** (`tests/unit/api.test.js`) : une fausse base (un objet qui renvoie un tableau de films) ; démarrez l'application sur le port `0` ; vérifiez le code 200 et le JSON de `/api/movies` ;
   - **intégration** (`tests/integration/api.test.js`) : même test avec la vraie base `MONGO_URL`.

- [ ] Le test unitaire tourne sans Mongo ni réseau
- [ ] Le test d'intégration lit réellement dans MongoDB
- [ ] `docker compose up --build` fonctionne toujours (l'application répond sur <http://localhost:8080>)

> **Aide : squelette d'un test d'API.** Démarrer l'application sur un port libre, l'appeler avec `fetch`, puis l'arrêter :
> ```js
> let server, baseUrl
> beforeAll(async () => {
>   const app = createApp({ getDb: () => /* votre base (fausse ou vraie) */ })
>   await new Promise(resolve => { server = app.listen(0, resolve) })
>   baseUrl = `http://localhost:${server.address().port}`
> })
> afterAll(() => new Promise(resolve => server.close(resolve)))
> ```
> Une fausse base pour `/api/movies` peut se réduire à `{ collection: () => ({ find: () => ({ toArray: async () => films }) }) }`.

## Partie 4 — Les jobs de tests en CI (15 min)

1. Ajoutez au stage `test` un job `unit-tests` (`npm run test:unit`).
2. Ajoutez un job `integration-tests` avec un **service** `mongo:8.0`. Dans le job, `MONGO_URL` doit viser `mongodb://mongo:27017/moviejs-test` : le nom du service sert de nom d'hôte.
3. Poussez et regardez les trois jobs du stage `test` s'exécuter en parallèle.

- [ ] Les jobs `lint`, `unit-tests`, `integration-tests` sont verts
- [ ] Vous savez expliquer pourquoi l'hôte s'appelle `mongo` en CI et `localhost` en local

## Partie 5 — Couverture et seuil bloquant (10 min)

1. Installez `@vitest/coverage-v8` et ajoutez le script `test:coverage` (`vitest run --coverage`).
2. Dans `vitest.config.js`, configurez `coverage` : fournisseur `v8`, fichiers inclus `src/utils.js`, `src/fetch.js`, `api/app.js`, et des **seuils** (par exemple 60 % de lignes et d'instructions, 70 % de fonctions, 80 % de branches).
3. Ajoutez un job `coverage` (avec le service Mongo) qui lance `npm run test:coverage`, avec `coverage: '/All files\s*\|\s*([\d.]+)/'` pour afficher le pourcentage dans la MR.
4. Essayez : montez un seuil à 90 % et constatez l'échec ; remettez la bonne valeur.

- [ ] Le pourcentage de couverture s'affiche dans la Merge Request
- [ ] Un seuil trop haut fait échouer le job

> Une couverture élevée ne prouve pas que les tests sont bons : elle montre seulement quel code est exécuté.

## Partie 6 — Protéger `main` (10 min)

Dans votre fork :
1. **Settings → Repository → Protected branches** : `main` est protégée (personne ne pousse directement).
2. **Settings → Merge requests** : activez l'exigence « **Pipelines must succeed** ».
3. Ouvrez une MR depuis `s3-tests` : le bouton de fusion doit rester bloqué tant que le pipeline n'est pas vert.
4. Cassez volontairement un test, constatez le blocage, corrigez, fusionnez.

- [ ] Impossible de fusionner une MR dont le pipeline est rouge
- [ ] La MR est fusionnée dans `main` de votre fork

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| `ECONNREFUSED 127.0.0.1:27018` en local | MongoDB n'est pas démarré, ou le port n'est pas publié | `docker compose up -d db` ; vérifiez `"27018:27017"` dans `docker-compose.yml` |
| `MongoServerSelectionError` en CI | `MONGO_URL` vise `localhost` au lieu de `mongo` | dans le job : `MONGO_URL: "mongodb://mongo:27017/moviejs-test"` |
| `Cannot find package 'express'` dans les tests | `express` n'est pas installé à la racine | `npm install --save-dev express` |
| les tests d'intégration passent une fois, échouent la 2e | base non nettoyée | videz la collection avant et après |
| `Coverage for lines (X%) does not meet global threshold` | le seuil est supérieur à la couverture réelle | ajoutez des tests, ou ajustez le seuil au niveau réel (pas à 100 %) |
| `EADDRINUSE` | deux serveurs sur le même port | démarrez sur le port `0` dans les tests |
| `coverage/` apparaît dans `git status` | dossier de rapports non ignoré | ajoutez `coverage/` à `.gitignore` et ne le committez pas |
| la MR peut être fusionnée malgré un pipeline rouge | « Pipelines must succeed » non activé | **Settings → Merge requests** |

## Défis bonus
1. Ajoutez un test de l'API pour un cas d'erreur (la base répond avec une erreur) : que doit renvoyer la route ?
2. Écrivez un test volontairement **flaky** (qui dépend de l'heure) : pourquoi est-ce un problème en CI ?

---

# CORRIGÉS

Branche de référence : `s3-tests` du dépôt de corrections. Éléments clés :

**Partie 3 — `api/app.js`**
```js
import express from 'express'

export function createApp({ getDb }) {
  const app = express()
  app.get('/api/movies', async (req, res) => {
    const movies = await getDb().collection('movies').find({}, { projection: { _id: 0 } }).toArray()
    res.json(movies)
  })
  return app
}
```
Dans `server.js` : `const app = createApp({ getDb: () => db })`.

**Partie 4 — jobs**
```yaml
unit-tests:
  stage: test
  script: [npm run test:unit]

integration-tests:
  stage: test
  services: [mongo:8.0]
  variables:
    MONGO_URL: "mongodb://mongo:27017/moviejs-test"
  script: [npm run test:integration]
```
L'hôte vaut `mongo` parce que GitLab nomme le service d'après l'image (partie avant le `:`) ; en local, c'est la machine qui expose le port 27018.

**Partie 5 — seuils** (`vitest.config.js`)
```js
coverage: {
  provider: 'v8',
  reporter: ['text', 'lcov'],
  include: ['src/utils.js', 'src/fetch.js', 'api/app.js'],
  thresholds: { lines: 60, statements: 60, functions: 70, branches: 80 },
},
```
`main.js` (manipulation du DOM) est volontairement hors du seuil : on évite de viser 100 % sur du code sans tests dédiés.

**Partie 6** : si le bouton « Protected branches » est inaccessible, votre rôle dans le projet est insuffisant : prévenez l'enseignant.

**Bonus**
1. Express 4 ne capture pas les erreurs d'une route `async` : une erreur de base laisse la requête sans réponse. Il faut un `try/catch` qui répond 500 (ou un gestionnaire d'erreurs).
2. Un test qui échoue « parfois » est ignoré ou relancé jusqu'à passer : la pipeline perd sa valeur de garde-fou.
