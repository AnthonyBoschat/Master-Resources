# Séance 6 — La chaîne d'approvisionnement : Trivy, SBOM, signature

**Durée : 1 h 30.** **Livrable :** des scans de dépendances et d'images bloquants, un SBOM et une signature pour chaque image publiée.

## Notions de la séance

- **Chaîne d'approvisionnement logicielle** : ce que contient votre application ne vient pas que de vous (dépendances npm, image de base, outils de CI). Chaque maillon peut être attaqué. En mars 2026, une version piégée de Trivy et des tags d'action réécrits ont volé des secrets de pipelines : même un outil de sécurité est une dépendance.
- **SCA et analyse d'image** : le *SCA* lit `package-lock.json` et signale les bibliothèques vulnérables ; l'*analyse d'image* inspecte le système et les paquets de l'image finale. Les gravités vont de `LOW` à `CRITICAL` ; on **bloque** à partir d'un seuil (`--exit-code 1`). Trivy ignore par défaut les dépendances de développement, qui ne sont pas livrées.
- **SBOM** (*Software Bill of Materials*) : l'inventaire des composants d'une image (formats SPDX ou CycloneDX). Quand une faille est annoncée, il permet de savoir en quelques secondes si l'on est concerné ; c'est aussi une exigence croissante de la réglementation européenne.
- **Signature sans clé** (*keyless*) : cosign obtient, avec le jeton OIDC fourni par GitLab (`id_tokens`), un certificat éphémère auprès de Sigstore (Fulcio) et enregistre la signature dans un journal public (Rekor). Aucune clé à stocker, donc aucune clé à voler. Elle prouve **qui** a produit l'image, pas qu'elle est exempte de failles.
- **Digest et tag** : un tag peut être réécrit pour pointer vers un autre contenu ; un digest identifie le contenu exact. On signe donc le digest.
- **Épingler ses outils** : `trivy:latest` change sans prévenir. On fixe une version précise (idéalement un digest), on met à jour volontairement.
- **`needs` et artefacts** : un job qui `needs` un autre peut démarrer dès que celui-ci est fini et récupère ses artefacts (ici `digest-api.txt`).
- **Noms de jobs imposés** (ils servent à l'évaluation) : `trivy-deps` ; `trivy-image-api` et `trivy-image-web` ; `sbom-api` et `sbom-web` ; `sign-image-api` et `sign-image-web`.

## Sommaire

| Partie | Durée | Contenu |
|---|---|---|
| 1 | 15 min | Trivy : les dépendances |
| 2 | 20 min | Trivy : les images publiées |
| 3 | 20 min | Le SBOM avec syft |
| 4 | 25 min | Signer avec cosign (OIDC) |
| 5 | 10 min | Épingler ses outils |

Créez une branche `s6-chaine` à partir de `main`.

---

## Partie 1 — Trivy sur les dépendances (15 min)

1. Ajoutez un job `trivy-deps` au stage `test` : image `aquasec/trivy:0.76.1` (`entrypoint: [""]`), script `trivy fs --scanners vuln --severity HIGH,CRITICAL --exit-code 1 .`
2. Lancez `npm audit` en local : il signale des vulnérabilités que Trivy ne bloque pas. Pourquoi ? (Indice : regardez le message de Trivy sur les dépendances de développement.)

- [ ] Le job `trivy-deps` est vert
- [ ] Vous savez justifier de ne bloquer que ce qui est livré

## Partie 2 — Trivy sur les images (20 min)

Ajoutez un stage `security` après `package`. Pour chacune des images `api` et `web`, un job qui analyse l'image **publiée dans le registre** :
- variables `TRIVY_USERNAME: $CI_REGISTRY_USER` et `TRIVY_PASSWORD: $CI_REGISTRY_PASSWORD` ;
- script `trivy image --severity CRITICAL --exit-code 1 "$CI_REGISTRY_IMAGE/api:$CI_COMMIT_SHORT_SHA"` ;
- mêmes `rules` que le job de build (branche principale et tags).

- [ ] Les deux jobs d'analyse d'image sont verts
- [ ] Vous savez expliquer pourquoi on analyse l'image publiée et non le Dockerfile

## Partie 3 — Le SBOM (20 min)

Un **SBOM** (*Software Bill of Materials*) est la liste des composants d'une image. Il permet de savoir, quand une faille est annoncée, si vous êtes concerné.

1. Ajoutez deux jobs `sbom-api` et `sbom-web` au stage `security`, avec l'image `anchore/syft:v1.55.0-debug` (`entrypoint: [""]` ; la variante `debug` contient un shell, la version par défaut n'en a pas).
2. Le binaire est à la racine : `/syft "registry:$CI_REGISTRY_IMAGE/api:$CI_COMMIT_SHORT_SHA" -o spdx-json=sbom-api.spdx.json`. Pour s'authentifier au registre, créez un fichier `config.json` Docker contenu dans `DOCKER_CONFIG` (voir le corrigé).
3. Conservez le fichier en **artefact** pendant 30 jours.

- [ ] Les deux SBOM sont téléchargeables depuis les artefacts
- [ ] Vous retrouvez la liste des paquets Alpine et npm dans le fichier

## Partie 4 — Signer avec cosign (25 min)

Une signature prouve qui a produit l'image. Avec la signature **sans clé** (*keyless*), aucune clé n'est à stocker : GitLab fournit un jeton OIDC éphémère, Sigstore l'échange contre un certificat valable quelques minutes.

1. Dans chaque job de build, enregistrez le **digest** de l'image publiée : `docker image inspect --format '{{index .RepoDigests 0}}' <image> > digest-api.txt`, en artefact. On signe le digest (immuable), pas le tag (déplaçable).
2. Ajoutez deux jobs `sign-image-api` et `sign-image-web` : image `alpine:3`, `needs` vers le job de build, `id_tokens: SIGSTORE_ID_TOKEN: aud: sigstore`, installation de cosign (`apk add --no-cache cosign`), `cosign login`, puis `cosign sign --yes "$(cat digest-api.txt)"`.
3. Vérifiez la signature depuis votre machine avec `cosign verify` en précisant l'identité attendue (voir le corrigé).

- [ ] Les jobs de signature sont verts
- [ ] `cosign verify` confirme la signature de votre image
- [ ] Vous savez dire ce que prouve (et ne prouve pas) une signature

## Partie 5 — Épingler ses outils (10 min)

En mars 2026, des tags de l'action Trivy ont été réécrits pour voler les secrets des pipelines. Relisez votre `.gitlab-ci.yml`.

1. Relevez toute image avec un tag flottant (`latest`, `docker:27`…).
2. Pour chacune, décidez : version précise suffisante, ou digest (`image@sha256:…`) ?

- [ ] Vous avez la liste de vos images et leur degré d'épinglage
- [ ] Vous savez expliquer le risque d'un tag flottant

## Erreurs fréquentes

| Symptôme | Cause probable | Remède |
|---|---|---|
| Trivy : `UNAUTHORIZED` sur l'image du registre | identifiants absents | variables `TRIVY_USERNAME` et `TRIVY_PASSWORD` |
| Trivy échoue sur une faille sans correctif disponible | vulnérabilité non corrigeable | `--ignore-unfixed` si l'équipe l'accepte, **et on le documente** |
| `sh: not found` dans le job syft | image sans shell | utilisez le tag `...-debug` |
| syft ne peut pas lire l'image | authentification absente | fichier `config.json` Docker et variable `DOCKER_CONFIG` |
| cosign : `no ambient OIDC credentials` | `id_tokens` absent, ou audience incorrecte | `id_tokens: SIGSTORE_ID_TOKEN: aud: sigstore` |
| `digest-api.txt: No such file` | `needs` ou artefact manquant | `needs: ["build-image-api"]` et `artifacts:` dans le job de build |
| signature refusée (`401` registre) | `cosign login` oublié | ajoutez-le avant `cosign sign` |
| le job passe alors qu'il y a des failles | l'échec est toléré (`allow_failure`) | retirez-le : un contrôle qui ne bloque pas ne protège pas |

## Défis bonus
1. Joignez le SBOM à l'image avec `cosign attest`.
2. Faites échouer `trivy-image-web` en choisissant volontairement une image de base ancienne : lisez le rapport.

---

# CORRIGÉS

Branche de référence : `s6-supply-chain`.

**Partie 3 — authentification de syft**
```yaml
before_script:
  - mkdir -p /tmp/docker-config
  - printf '{"auths":{"%s":{"auth":"%s"}}}' "$CI_REGISTRY" "$(printf '%s' "$CI_REGISTRY_USER:$CI_REGISTRY_PASSWORD" | base64 -w0)" > /tmp/docker-config/config.json
  - export DOCKER_CONFIG=/tmp/docker-config
```

**Partie 4 — signature**
```yaml
sign-image-api:
  stage: security
  image: alpine:3
  needs: ["build-image-api"]
  id_tokens:
    SIGSTORE_ID_TOKEN:
      aud: sigstore
  before_script:
    - apk add --no-cache cosign
    - cosign login "$CI_REGISTRY" -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD"
  script:
    - cosign sign --yes "$(cat digest-api.txt)"
```
Vérification (à adapter au chemin de votre projet ; syntaxe à valider avec la documentation GitLab sur Sigstore) :
```bash
cosign verify <image>@<digest> \
  --certificate-identity-regexp '^https://gitlab.com/<chemin-du-projet>//.gitlab-ci.yml@refs/.*$' \
  --certificate-oidc-issuer https://gitlab.com
```
Une signature prouve l'**origine** (cette pipeline de ce projet), pas l'**absence de vulnérabilités**.

**Partie 1** : Trivy ignore par défaut les dépendances de développement (Vitest, Vite…), qui ne sont pas livrées.

**Partie 5** : un digest est immuable, un tag peut être réécrit. Pour les images de base comme `docker:27`, une version précise est un minimum ; le digest est plus sûr.
