#!/bin/bash
# Empaqueta BrickGraphics en una carpeta lista para compartir y genera un ZIP.
# Requiere: bash, zip, jar y el JDK en el PATH.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

DIST_ROOT="dist"
TIMESTAMP="$(date +%Y%m%d)"
DIST_NAME="BrickGraphics_${TIMESTAMP}"
DIST_DIR="${DIST_ROOT}/${DIST_NAME}"

mkdir -p "$DIST_ROOT"

# 1) Compilar el proyecto para asegurar clases recientes
./COMPILAR.command

# 2) Preparar carpeta limpia
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

COPY_PATHS=(
  "bin"
  "lddmc.kvm"
  "lddmc_layers"
  "color_translations"
  "icons"
  "about.txt"
  "color_groups.txt"
  "colors.txt"
  "sources.txt"
  "COMO_USAR_NUMEROS_PERSONALIZADOS.txt"
  "GUIA_INTERFAZ_NUMEROS_PERSONALIZADOS.md"
  "RESUMEN_NUMEROS_PERSONALIZADOS.md"
  "LAYER_SYSTEM_SUMMARY.md"
  "LAYER_REVERSIBLE_PERSISTENCE.md"
  "README.md"
  "RELEASE_NOTES.md"
  "BrickGraphics_Launcher.command"
  "BrickGraphics_Windows.bat"
  "Ejecutar_BrickGraphics.command"
  "Ejecutar_BrickGraphics_DESARROLLO.command"
  "run_griddy.bat"
  "make.bat"
)

for path in "${COPY_PATHS[@]}"; do
  if [ -e "$path" ]; then
    rsync -a "$path" "$DIST_DIR/"
  else
    echo "⚠️  Advertencia: $path no existe y se omitirá del paquete"
  fi
done

# 3) Generar un JAR autocontenido dentro de la carpeta distribuible
jar --create \
    --file "$DIST_DIR/BrickGraphics.jar" \
    --main-class program.JWrapper \
    -C bin .

# 4) Limpiar archivos temporales del paquete
find "$DIST_DIR" -name ".DS_Store" -delete || true
find "$DIST_DIR" -name "*.log" -delete || true

# Quitar artefactos de pruebas que no deben distribuirse
UNNEEDED_FILES=(
  "bin/lddmc_isolation_test.kvm"
  "bin/test_custom_ids.txt"
)

for file in "${UNNEEDED_FILES[@]}"; do
  rm -f "$DIST_DIR/$file"
done

# 5) Comprimir en ZIP para facilitar envío
pushd "$DIST_ROOT" >/dev/null
zip -r "${DIST_NAME}.zip" "$DIST_NAME" >/dev/null
popd >/dev/null

cat <<EOF
✅ Carpeta distribuible creada en: $DIST_DIR
✅ Archivo ZIP listo para compartir: ${DIST_ROOT}/${DIST_NAME}.zip
EOF
