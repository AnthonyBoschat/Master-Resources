# Séance 2 — Votre première vraie pipeline

**Durée : 1 h 30** (travaux pratiques, après la partie théorique).
**Livrable :** une pipeline verte sur une Merge Request de votre fork : un stage `build` et un stage `test` avec le lint.

## Notions de la séance

- **Structure de `.gitlab-ci.yml`** : `stages` fixe l'ordre des étapes ; chaque job a un `stage`, une `image` (l'environnement d'exécution) et un `script` (les commandes). Un job échoue dès qu'une commande renvoie un code différent de 0.
- **`npm ci` plutôt que `npm install`** : `npm ci` installe exactement ce que fige `package-lock.json` et échoue si le fichier est désynchronisé. Même résultat sur votre machine, en CI et dans l'image : c'est la reproductibilité.
- **Le lint (ESLint)** : analyse le code sans l'exécuter pour détecter erreurs et écarts de style (`var`, variables inutilisées…). En CI, il garantit que personne ne fusionne du code qui viole les règles du projet.
- **Cache et artefacts** : le *cache* accélère les pipelines suivants et n'est pas garanti ; les *artefacts* transmettent un résultat à un autre job (ici `node_modules/`). Le cache de npm porte sur `.npm/` (dossier de téléchargement) et non sur `node_modules/`, que `npm ci` supprime et recrée.
- **Pourquoi `HUSKY: "0"`** : le script `prepare` de `package.json` installe un hook git (Husky), qui a besoin de `git`, absent de l'image `alpine`. En CI, les hooks n'ont aucun sens.
- **Pipelines de branche et de Merge Request** : le premier se lance à chaque push ; le second est rattaché à la MR (variables `CI_MERGE_REQUEST_*`, contrôle de fusion). `workflow:rules` décide quels pipelines sont créés.
- **Valider un fichier CI** : l'éditeur de pipeline de GitLab (**Build → Pipeline editor**) signale les erreurs de syntaxe avant le push.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 1 | 15 min | Le stage `build` |
| 2 | 15 min | Le stage `test` : le lint |
| 3 | 20 min | Une seule pipeline par push : `workflow:rules` |
| 4 | 20 min | Le cache npm |
| 5 | 20 min | Ouvrir une Merge Request et provoquer un échec |

Partez de votre branche `s1-atelier` (ou créez `s2-pipeline` depuis `main`). Vous modifiez le fichier `.gitlab-ci.yml` de la séance 1.

---

## Partie 1 — Le stage `build` (15 min)

1. Déclarez deux stages : `build`, puis `test`.
2. Choisissez l'image par défaut : `node:24-alpine`.
3. Créez un job `build` qui installe les dépendances avec `npm ci`.
4. Dans les variables, ajoutez `HUSKY: "0"`. Le script `prepare` de `package.json` lance Husky, qui a besoin de `git`, absent de l'image `alpine`. Sans cette variable, `npm ci` échoue.
5. Transmettez `node_modules/` au stage suivant avec `artifacts` et une durée `expire_in` d'une heure.

**Critères de réussite**
- [ ] Le job `build` est vert
- [ ] Vous voyez `node_modules/` dans les **artefacts** du job (bouton Parcourir)
- [ ] Vous savez expliquer pourquoi `HUSKY: "0"` est nécessaire

## Partie 2 — Le stage `test` : le lint (15 min)

1. Ajoutez un job `lint` dans le stage `test`, avec la commande `npm run lint`.
2. Poussez et regardez l'ordre d'exécution : `lint` ne démarre qu'après `build`.

**Critères de réussite**
- [ ] Le pipeline contient deux jobs, dans deux stages
- [ ] Le job `lint` utilise `node_modules/` reçu de `build` (il ne réinstalle rien)

## Partie 3 — Pipelines de branche et de Merge Request (20 min)

Un **pipeline de branche** se déclenche à chaque push. Un **pipeline de Merge Request** se déclenche pour la MR : il dispose des variables `CI_MERGE_REQUEST_*` et c'est lui qui conditionne la fusion. Il faut l'autoriser explicitement.

1. Ouvrez une MR depuis votre branche vers `main` **dans votre fork**. Regardez le type de pipeline affiché dans la MR : pour l'instant, c'est celui de la branche.
2. Ajoutez en tête de fichier le bloc `workflow:rules` vu en cours, qui :
   - accepte les pipelines de Merge Request ;
   - refuse la pipeline de branche quand une MR est ouverte (sinon on aurait deux pipelines pour un même push) ;
   - accepte les autres branches et les tags.
3. Poussez à nouveau et comparez.

**Critères de réussite**
- [ ] Vous avez observé la pipeline de branche dans la MR avant la correction
- [ ] Après correction, la MR affiche un pipeline de Merge Request
- [ ] Un push lance **un seul** pipeline
- [ ] Vous savez expliquer pourquoi on distingue les deux types

## Partie 4 — Le cache npm (20 min)

Les artefacts livrent un résultat à un autre job. Le **cache** accélère les pipelines suivants.

1. Au job `build`, ajoutez `--cache .npm --prefer-offline` à la commande `npm ci`.
2. Déclarez un `cache` sur le dossier `.npm/`, avec une clé calculée à partir de `package-lock.json`.
3. Lancez deux pipelines à la suite sans changer les dépendances et comparez la durée du job `build`.

**Critères de réussite**
- [ ] Au deuxième pipeline, le journal indique que le cache a été restauré
- [ ] Vous savez dire pourquoi on met en cache `.npm/` et non `node_modules/`

## Partie 5 — Provoquer un échec (20 min)

1. Introduisez une faute de lint dans `src/main.js` (par exemple une variable `var`).
2. Poussez : le job `lint` doit échouer et **bloquer** la pipeline.
3. Corrigez, poussez, observez le retour au vert.
4. Fusionnez la MR dans `main` **de votre fork**.

**Critères de réussite**
- [ ] Vous avez vu un pipeline rouge, puis vert
- [ ] La MR est fusionnée dans `main` de votre fork

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| `yaml invalid`, `config should be a hash`, ou aucun job créé | indentation YAML (la cause n°1) | des **espaces**, jamais de tabulations ; validez dans l'éditeur de pipeline |
| `npm ci` échoue : `package.json and package-lock.json are not in sync` | dépendances modifiées sans mettre à jour le lock | `npm install` en local, puis committez `package-lock.json` |
| `npm ci` échoue sur `husky` / `git: not found` | `HUSKY` non désactivé | `variables: HUSKY: "0"` |
| le job `lint` échoue | une règle ESLint est violée | lisez la ligne et la règle dans le journal ; corrigez le code, ne désactivez pas la règle |
| `lint` ne trouve pas `eslint` | `node_modules/` absent | vérifiez l'`artifacts` du job `build` |
| un job ne démarre jamais | `rules` ou `stage` mal écrits, ou runner indisponible | relisez la section `rules` ; vérifiez le runner |
| le cache n'est pas restauré au 1er pipeline | normal : il n'existe pas encore | relancez un 2e pipeline sans changer `package-lock.json` |
| deux pipelines pour un même push | `workflow:rules` absent ou incomplet | ajoutez le bloc vu en cours |

## Défis bonus

1. Ajoutez un job `audit` dans le stage `test`, qui lance `npm audit --omit=dev` : que se passe-t-il s'il trouve une vulnérabilité ? Faut-il qu'il bloque ?
2. Lancez `lint` en même temps que `build` en utilisant `needs`. Qu'y gagnez-vous, et que perdez-vous ?

---

# CORRIGÉS

## Parties 1 à 4
Le fichier complet se trouve dans `corriges/.gitlab-ci.yml`. Les points à retenir :
- `HUSKY: "0"` désactive le script `prepare`, qui exécute `husky` et a besoin de `git`.
- `artifacts` livre `node_modules/` au stage suivant ; `cache` sur `.npm/` évite de retélécharger les paquets. `node_modules/` n'est pas mis en cache parce que `npm ci` le supprime et le recrée à chaque fois.
- La clé de cache est calculée sur `package-lock.json` : elle change quand les dépendances changent.

## Partie 3
```yaml
workflow:
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH && $CI_OPEN_MERGE_REQUESTS
      when: never
    - if: $CI_COMMIT_BRANCH
    - if: $CI_COMMIT_TAG
```

## Bonus
1. Par défaut un job qui échoue bloque la pipeline. Un `npm audit` bloquant rend la pipeline fragile, car une nouvelle vulnérabilité publiée apparaît du jour au lendemain, sans qu'on ait touché au code. On l'étudie en séance 5.
2. Avec `needs: []`, `lint` démarre tout de suite, mais il n'a plus `node_modules/` : il doit réinstaller. Le gain de parallélisme se paye en redondance.
