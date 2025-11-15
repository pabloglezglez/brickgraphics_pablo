#!/bin/bash
# Script de compilación rápida para desarrollo

cd "$(dirname "$0")"

echo "🔧 Compilando BrickGraphics..."

# Matar procesos Java anteriores
echo "🛑 Terminando procesos Java anteriores..."
pkill -f java 2>/dev/null || true

# Crear directorio bin si no existe
mkdir -p bin

# Compilar todo el proyecto
echo "📦 Compilando código fuente..."
if find src -name "*.java" -exec javac -cp "src:." -d bin {} + 2>/dev/null; then
    echo "✅ Compilación exitosa"
    echo ""
    echo "🚀 Para ejecutar:"
    echo "   ./Ejecutar_BrickGraphics_DESARROLLO.command"
    echo "   O manualmente: java -cp '.:bin' program.JWrapper"
else
    echo "❌ Error en compilación"
    echo "🔍 Verifica errores arriba y corrige el código"
    exit 1
fi