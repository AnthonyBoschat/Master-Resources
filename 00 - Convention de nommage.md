# Convention de nommage

## Dossiers

Un dossier par cours, nommé avec le titre du cours.
Majuscule au début uniquement, sauf noms propres (Docker, Jetpack Compose).

Exemples :
- `Algorithmique et complexité`
- `CI-CD - Intégration et déploiement continus`

## Fichiers

Format :

```
NN - Type - Titre.ext
```

| Élément | Règle |
|---|---|
| `NN` | Ordre de lecture sur 2 chiffres : `00`, `01`, `02`… `00` = préparatif, test de positionnement ou support global |
| `Type` | Un type de la liste ci-dessous |
| `Titre` | Titre réel du document, en français, court. Majuscule au début uniquement |
| `.ext` | Extension d'origine, inchangée |

Pour un TP numéroté par l'intervenant : `NN - TP X - Titre` (ex : `03 - TP 1 - Les structures de contrôle.pdf`).

Plusieurs fichiers d'une même ressource gardent le même numéro et le même titre (ex : énoncé `.pdf` + notebook `.ipynb`).

## Types

| Type | Contenu |
|---|---|
| `Cours` | Diaporama ou support théorique |
| `TP` | Énoncé de travaux pratiques |
| `Activité` | Exercice de groupe, atelier, mise en situation |
| `Exercices` | Feuille d'exercices |
| `Devoir` | Travail à rendre |
| `Fiche` | Fiche récapitulative, tableau, mémo |
| `Complément` | Support facultatif pour approfondir |
| `Test` | Test de positionnement, évaluation |

Si deux versions d'un même cours existent, préciser entre parenthèses : `(slides)`, `(support)`.

## Caractères

- Séparateur : ` - ` (espace, tiret, espace).
- Pas de `°`, pas de `_`, pas de majuscules partout.
- Accents autorisés.

## Exceptions : ne pas renommer

- Code source (`.java`, etc.) : le nom du fichier doit correspondre à la classe.
- Données (`.csv`, `.json`…) utilisées par un notebook ou un script : le code les appelle par leur nom.
- Sous-dossiers `Corrections/` et `Sources/` : gardent leur structure.

## Doublons

Avant d'ajouter un fichier, comparer le contenu (pas seulement le nom).
Les doublons sont déplacés dans `_doublons/`, exclu du zip.

## Ajouter un fichier

1. Le placer dans le dossier du cours.
2. Lui donner le numéro suivant, ou l'insérer à sa place chronologique et renuméroter.
3. Vérifier qu'il n'est pas un doublon.
4. Régénérer le zip sans `_doublons/`.
