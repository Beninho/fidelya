# Déploiement Fidelya sur Google Play Store

**Date :** 2026-04-17  
**App :** `com.beninho.fidelya` — versionCode 1, versionName 1.0  
**Format :** AAB (Android App Bundle)  
**Cible :** Production directe  
**Workflow :** Android Studio (manuel)

---

## 1. Prérequis & Signing

### Étape 1 — Ajouter les credentials dans `local.properties`

`local.properties` est ignoré par git (déjà dans `.gitignore` par défaut Android Studio) :

```properties
# local.properties
KEYSTORE_PASSWORD=ton_store_password
KEY_ALIAS=ton_key_alias
KEY_PASSWORD=ton_key_password
```

### Étape 2 — Modifier `app/build.gradle.kts`

Ajouter l'import et la lecture de `local.properties` **au top du fichier**, puis le `signingConfig` dans le bloc `android {}` :

```kotlin
// app/build.gradle.kts — début du fichier
import java.util.Properties

val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

plugins {
    // ... alias existants inchangés
}

android {
    // ... namespace, compileSdk, defaultConfig inchangés

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/fidelya.jks")  // adapter au chemin réel de ta keystore
            storePassword = localProps["KEYSTORE_PASSWORD"] as String
            keyAlias = localProps["KEY_ALIAS"] as String
            keyPassword = localProps["KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")  // ajouter cette ligne
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 2. Build AAB via Android Studio

1. Ouvrir **Build > Generate Signed Bundle / APK**
2. Sélectionner **Android App Bundle**
3. Choisir la keystore existante → renseigner alias + passwords
4. Sélectionner variant **release**
5. Cliquer **Finish**

AAB généré dans : `app/release/app-release.aab`

### Checklist avant chaque build

- [ ] `versionCode` incrémenté (unique par upload Play)
- [ ] `versionName` mis à jour (ex: "1.0", "1.1")
- [ ] Tests unitaires passants
- [ ] Build release testé sur device physique ou émulateur

---

## 3. Préparation de la fiche Play Console

### Informations obligatoires

| Champ | Contrainte |
|-------|-----------|
| Nom de l'app | Max 30 caractères |
| Description courte | Max 80 caractères |
| Description longue | Max 4000 caractères |
| Icône | 512×512px, PNG, fond non transparent |
| Screenshots phone | Min 2, max 8 |
| Catégorie | Ex: Shopping / Lifestyle |
| Email de contact | Visible sur la fiche |

### Politique de confidentialité

Google exige une URL de politique de confidentialité pour toute app sur le Play Store, **même si l'app ne collecte aucune donnée**.

**Contenu minimal à inclure :**
- Quelles données sont collectées (ou confirmation qu'aucune ne l'est)
- Comment elles sont utilisées et stockées
- Si elles sont partagées avec des tiers
- Contact de l'éditeur

**Cas de Fidelya :**
- CameraX → accès caméra → mentionner que la caméra sert uniquement à scanner des codes-barres, aucune image n'est envoyée ni stockée
- Room → stockage local uniquement → toutes les données restent sur l'appareil, aucune transmission vers un serveur
- Aucun compte utilisateur, aucun tracking → le mentionner explicitement

**Rédiger le document :**
Utiliser un générateur gratuit comme [privacypolicygenerator.info](https://privacypolicygenerator.info) ou rédiger manuellement un document simple en Markdown/HTML.

**Héberger l'URL :**
- **GitHub Pages** (gratuit) : créer un repo `fidelya-privacy` avec un `index.html`, activer Pages → URL type `https://beninho.github.io/fidelya-privacy`
- **Notion** : créer une page publique → copier le lien de partage
- Tout hébergement statique accessible publiquement fonctionne

**Renseigner l'URL dans Play Console :**
Fiche de l'app > **Politique de confidentialité** → coller l'URL. Aussi à renseigner dans le questionnaire sur la sécurité des données (section séparée de la fiche).

### Classification du contenu (IARC)

- Remplir le questionnaire IARC dans Play Console
- Obtenir une note de contenu avant soumission en production
- Durée : ~5 minutes

---

## 4. Upload & Publication

### Étapes sur Play Console

1. Aller dans **Production > Créer une nouvelle version**
2. **Uploader** `app-release.aab`
3. Play Console vérifie la signature (doit correspondre à l'app signing key enregistrée)
4. Renseigner les **notes de version** (obligatoire, min 1 langue, ex: "Première version publique de Fidelya")
5. Cliquer **Enregistrer** puis **Vérifier la version**
6. Corriger les éventuels warnings signalés par Play Console
7. Cliquer **Envoyer en examen**

### Délais

| Étape | Durée estimée |
|-------|--------------|
| Review Google (1ère soumission) | 1 à 3 jours ouvrés |
| Visibilité sur le Store après approbation | ~24h |

### Déploiement progressif (optionnel)

Après approbation, possibilité de déployer par pourcentage :
- 10% → surveiller crashs et avis
- 50% → si stable
- 100% → déploiement complet

---

## 5. Checklist globale avant soumission

- [ ] `signingConfig` release configuré et testé
- [ ] AAB buildé en variant **release**
- [ ] `versionCode = 1`, `versionName = "1.0"`
- [ ] Icône 512×512px prête
- [ ] Min 2 screenshots phone
- [ ] Description courte + longue rédigées
- [ ] Politique de confidentialité hébergée et URL renseignée
- [ ] Questionnaire IARC complété
- [ ] Notes de version rédigées
- [ ] AAB uploadé sur Play Console
- [ ] Version envoyée en examen

---

## 6. Pour les versions suivantes

À chaque nouvelle release :
1. Incrémenter `versionCode` (obligatoire, Play rejette si identique)
2. Mettre à jour `versionName`
3. Rebuild AAB signé
4. Upload sur Play Console dans **Production > Nouvelle version**
