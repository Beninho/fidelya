#!/usr/bin/env bash
#
# Génère les schémas Room dans app/schemas/.
#
# Room n'exporte que la version courante de la base. Le schéma v1 n'a donc
# jamais été écrit sur disque : il a été perdu quand la base est passée en v2.
#
# On peut le reconstituer sans rien inventer, parce que la v2 ne change aucune
# table — MIGRATION_1_2 ne touche que des valeurs. Les deux schémas sont donc
# identiques au numéro de version près, y compris l'identityHash, que Room
# calcule à partir des entités et non de la version.
#
# Ce script ne réécrit jamais un schéma déjà présent : une fois committés, ces
# fichiers sont l'historique de la base.

set -euo pipefail

cd "$(dirname "$0")/.."

SCHEMA_DIR="app/schemas/com.beninho.fidelya.data.db.AppDatabase"

echo "→ Export du schéma courant via KSP"
./gradlew :app:kspDebugKotlin

if [ ! -f "$SCHEMA_DIR/2.json" ]; then
  echo "✗ $SCHEMA_DIR/2.json absent — vérifier room.schemaLocation dans app/build.gradle.kts" >&2
  exit 1
fi

if [ -f "$SCHEMA_DIR/1.json" ]; then
  echo "✓ 1.json déjà présent, laissé tel quel"
else
  echo "→ Dérivation de 1.json depuis 2.json (schéma identique, version différente)"
  sed 's/"version": 2,/"version": 1,/' "$SCHEMA_DIR/2.json" > "$SCHEMA_DIR/1.json"
fi

echo "✓ Schémas prêts :"
ls -1 "$SCHEMA_DIR"
