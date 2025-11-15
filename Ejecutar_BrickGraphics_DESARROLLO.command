#!/bin/bash
# Script para DESARROLLO - Ejecuta desde código fuente compilado
# Usar este script cuando modifiques código fuente

cd "$(dirname "$0")"

# Verificar que existe el directorio bin
if [ ! -d "bin" ]; then
    echo "❌ Directorio 'bin' no encontrado. Ejecuta primero:"
    echo "   javac -cp 'src' -d bin src/program/JWrapper.java"
    exit 1
fi

# Verificar que existe la clase principal
if [ ! -f "bin/program/JWrapper.class" ]; then
    echo "❌ Clase principal no compilada. Ejecuta:"
    echo "   find src -name '*.java' -exec javac -cp 'src:.' -d bin {} +"
    exit 1
fi

echo "🚀 Ejecutando BrickGraphics desde código fuente..."
echo "📁 Usando archivos compilados en: bin/"
echo ""

# Ejecutar desde código fuente compilado
java -cp ".:bin" program.JWrapper