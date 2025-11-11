#!/usr/bin/env bash

# 🎮 SCRIPT DE INICIO RÁPIDO - SISTEMA OVERLAYS MÚLTIPLES

echo "🚀 INICIANDO SISTEMA DE OVERLAYS MÚLTIPLES"
echo "=========================================="
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "src/mosaic/OverlaySystemGUI.java" ]; then
    echo "❌ Error: No se encuentra el archivo del sistema"
    echo "   Asegúrate de estar en el directorio brickgraphics_pablo"
    exit 1
fi

echo "✅ Archivos del sistema encontrados"
echo "📁 Directorio: $(pwd)"
echo ""

echo "🔧 Compilando sistema de overlays..."
javac -cp "src:bin" src/mosaic/OverlaySystemGUI.java -d bin 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
else
    echo "⚠️  Usando versión previamente compilada"
fi

echo ""
echo "🎯 LANZANDO APLICACIÓN..."
echo ""
echo "┌─────────────────────────────────────────────────┐"
echo "│  📖 GUÍA RÁPIDA:                               │"
echo "│                                                 │"
echo "│  1. 🖼️  Cargar Imagen → Selecciona tu imagen   │"
echo "│  2. 📋 Añadir Overlay → Superpón más imágenes  │"
echo "│  3. 🎛️  Usar controles → Posición, opacidad    │"
echo "│  4. 🔄 Reordenar → Botones ↑↓🔝🔻             │"
echo "│  5. 👁️  Visible/Oculto → Checkbox             │"
echo "│                                                 │"
echo "│  💡 TIPS:                                      │"
echo "│   • Cada overlay es independiente               │"
echo "│   • Opacidad 0-100% en tiempo real             │"
echo "│   • Overlays ilimitados (según memoria)        │"
echo "│   • Monitor de memoria en la parte inferior    │"
echo "│                                                 │"
echo "└─────────────────────────────────────────────────┘"
echo ""
echo "🚀 ¡INICIANDO APLICACIÓN!"
echo ""

# Ejecutar la aplicación
java -cp "src:bin" mosaic.OverlaySystemGUI

# Mensaje al cerrar
echo ""
echo "👋 ¡Gracias por usar el Sistema de Overlays Múltiples!"
echo ""
echo "📚 Para más información, consulta:"
echo "   • COMO_USAR_OVERLAYS_MULTIPLES.md"
echo "   • INTEGRACION_BRICKGRAPHICS_COMPLETA.md"