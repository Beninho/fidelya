# Refaire les visuels du Play Store

Comment reproduire les huit captures de `screenshots/` et l'image de présentation,
puis les remplacer dans la fiche Play Console.

À relancer dès que le design system bouge : les captures d'une fiche périmée
montrent une app que personne ne télécharge — c'est ce qui est arrivé aux
premières, dessinées en PIL du temps de Material 3.

---

## 1. Émulateur

Un émulateur suffit, et vaut mieux qu'un téléphone : sa taille d'écran est
réglable, donc le ratio attendu par Play Store est atteignable sans recadrage.

```bash
~/Library/Android/sdk/emulator/emulator -avd <AVD> -no-snapshot-load -no-boot-anim &
scripts/prepare-emulator.sh      # déverrouille, coupe les animations
```

### Taille d'écran

L'AVD « Medium Phone » est en 1080×2400, soit du 9:20 : trop haut pour Play
Store, qui n'accepte pas au-delà de 2:1. On force le 9:16, densité comprise —
sans quoi le rendu grossit et la liste ne tient plus dans l'écran.

```bash
adb shell wm size 1080x1920
adb shell wm density 440
```

À la fin des captures, `adb shell wm size reset` et `adb shell wm density reset`.

### Status bar

Le mode démo de SystemUI fige l'heure et les indicateurs : sans lui, chaque
capture porte une heure différente et l'icône batterie de l'émulateur, ce qui se
voit dans la grille de la fiche.

```bash
adb shell settings put global sysui_demo_allowed 1
for c in "enter" \
         "clock -e hhmm 0941" \
         "battery -e level 100 -e plugged false" \
         "network -e wifi show -e level 4 -e fully true" \
         "network -e mobile hide" \
         "notifications -e visible false"; do
    adb shell am broadcast -a com.android.systemui.demo -e command $c
done
```

Sortie : `adb shell am broadcast -a com.android.systemui.demo -e command exit`.

---

## 2. Peupler la base

`ScreenshotSeeder` (dans `app/src/androidTest/.../screenshots/`) écrit six cartes
présentables et marque l'onboarding vu. C'est un utilitaire de capture, pas un
test de comportement : les images d'émulateur n'embarquent pas `sqlite3`, et Room
est le seul chemin d'écriture vers `fidelya.db`.

```bash
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w \
    -e class com.beninho.fidelya.screenshots.ScreenshotSeeder \
    com.beninho.fidelya.test/androidx.test.runner.AndroidJUnitRunner
```

**Ne pas passer par `./gradlew connectedDebugAndroidTest`** : la tâche désinstalle
les deux APK en fin de course, et la base part avec l'app. `am instrument` laisse
tout en place.

Les horodatages du seeder sont figés en dur (`now = 1_755_000_000_000`) : les
dates « Ajoutée le » et « Dernier passage » sont donc les mêmes d'une session à
l'autre, et les captures claire et sombre d'un même écran restent cohérentes.

---

## 3. Prendre les captures

```bash
adb shell am force-stop com.beninho.fidelya
adb shell am start -n com.beninho.fidelya/.MainActivity
adb exec-out screencap -p > 01_card_list.png
```

Laisser ~2 s après chaque navigation : sans délai, la capture attrape le
spinner de chargement.

| Fichier | Parcours depuis la liste |
|---|---|
| `01_card_list.png` | écran d'accueil |
| `02_card_detail.png` | taper une carte |
| `03_checkout_mode.png` | détail → « Présenter en caisse » |
| `04_scan.png` | « Ajouter une carte » (voir plus bas) |
| `05_card_edit.png` | « Saisir », formulaire rempli |
| `06_reorder.png` | « Ordre » |

Au premier passage en mode caisse, Android superpose son bulletin « Viewing full
screen » : taper « Got it », puis reprendre la capture. Il ne revient pas.

### Thème « Encre »

Le thème de l'app suit le système par défaut, donc rien à régler dans l'app :

```bash
adb shell cmd uimode night yes
adb shell am force-stop com.beninho.fidelya   # relancer pour repartir de la liste
```

`07_card_list_dark.png` et `08_card_detail_dark.png` en sortent. Le mode caisse
n'a pas de variante sombre — l'écran reste blanc par conception, pour que le
lecteur du magasin s'en sorte.

Capturer le détail **avant** d'entrer en mode caisse : le passage en caisse écrit
`lastUsedAt`, et « Dernier passage » ne collerait plus avec la capture claire.

Retour au clair : `adb shell cmd uimode night no`.

### Le cas de `04_scan.png`

L'écran de scan n'a pas d'habillage : il montre le flux caméra brut, c'est-à-dire
le salon 3D de la scène virtuelle de l'émulateur. Injecter une image dans cette
scène ne marche pas — `-virtualscene-poster wall=<png>` est accepté par
l'émulateur sans rien y changer.

La capture livrée est donc composite : le chrome de l'écran est réel, seul le
contenu du preview est incrusté (carte à code-barres sur fond sombre, ombre et
vignette). Le code-barres vient du recadrage d'une capture du mode caisse, ce qui
évite d'embarquer un encodeur EAN-13 pour l'occasion.

Si un jour la scène virtuelle accepte un poster, ou si les captures se font sur
un vrai téléphone braqué sur une vraie carte, cette composition n'a plus de
raison d'être.

### `screenshots_gen.py`

Le script qui dessinait les six écrans en PIL est laissé en place mais n'est plus
la source des captures : il rend la maquette Material 3 d'avant Modernist, palette
violette et coins arrondis comprises. Le relancer écraserait les vraies captures.

---

## 4. Image de présentation

```bash
python3 feature_graphic_gen.py     # depuis la racine : le chemin de la police est relatif
```

Sort `screenshots/feature_graphic.png` en 1024×500. Le script tient les tokens
Modernist en double des sources Kotlin (fond, encre, accent, fonds de cartes
issus de `ModernistPalette`, et `cardForegroundColor` reimplémenté pour l'encre
des tuiles). Un changement de palette dans l'app demande donc de repasser ici.

Archivo est lue directement dans `app/src/main/res/font/archivo.ttf` : c'est une
police variable, et PIL choisit le poids par `set_variation_by_name("ExtraBold")`.
PIL n'a pas d'interlettrage, d'où le rendu lettre à lettre de `tracked_text`.

---

## 5. Remplacer dans Play Console

Aucune automatisation dans le repo : l'upload est manuel.

1. [play.google.com/console](https://play.google.com/console) → app **Fidelya**
2. **Développer** → **Présence sur le Store** → **Fiche Play Store principale**
3. Sélectionner la langue **fr-FR** en haut : les visuels sont par langue, les
   autres langues retombent sur la langue par défaut
4. Section **Éléments graphiques** :
   - **Icône** → `fidelya_512.png`
   - **Image de présentation** → `screenshots/feature_graphic.png`
   - **Captures d'écran de téléphone** → les huit `01_…` à `08_…`
5. Supprimer les anciennes captures **avant** d'ajouter : le maximum est de huit,
   et l'ajout est refusé au-delà
6. L'ordre d'affichage est celui de la grille, réordonnable au glisser-déposer
7. **Enregistrer**. La fiche passe en revue (quelques heures à deux jours) ;
   l'ancienne reste en ligne pendant ce temps

### Contraintes Play Store

| Élément | Exigence | Ce qu'on livre |
|---|---|---|
| Captures téléphone | 2 à 8, PNG/JPG, côté 320–3840 px, ratio ≤ 2:1 | 8 × 1080×1920 PNG |
| Image de présentation | exactement 1024×500, sans transparence | 1024×500 PNG RGB |
| Icône | 512×512 PNG, fond opaque | `fidelya_512.png` |

Les deux captures sombres comptent dans les huit : Play Store n'a pas de slot
par thème. Les onglets Tablette et Chromebook restent vides — la fiche est
publiable ainsi, au prix d'une mention « non optimisée pour tablette ».
