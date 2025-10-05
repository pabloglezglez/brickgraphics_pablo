#!/bin/bash
# Script para ejecutar BrickGraphics desde cualquier ubicación
cd "$(dirname "$0")"

# Verificar si Java está instalado
if ! command -v java &> /dev/null; then
    echo "Java no está instalado o no está en el PATH"
    echo "Por favor instala Java y vuelve a intentar"
    read -p "Presiona Enter para salir..."
    exit 1
fi

# Verificar si el archivo JAR existe
if [ ! -f "BrickGraphics.jar" ]; then
    echo "Error: No se encuentra el archivo BrickGraphics.jar"
    echo "Asegúrate de que ambos archivos estén en la misma carpeta"
    read -p "Presiona Enter para salir..."
    exit 1
fi

# Ejecutar la aplicación
echo "Iniciando BrickGraphics..."
java -jar BrickGraphics.jar

# Mantener la ventana abierta si hay error
if [ $? -ne 0 ]; then
    echo "Error al ejecutar la aplicación"
    read -p "Presiona Enter para salir..."
fi