#!/bin/bash
# DEVELOPMENT script - runs from compiled source code
# Use this script whenever you change source code

cd "$(dirname "$0")"

# Ensure the bin directory exists
if [ ! -d "bin" ]; then
    echo "❌ Directory 'bin' not found. Run first:"
    echo "   javac -cp 'src' -d bin src/program/JWrapper.java"
    exit 1
fi

# Ensure the main class exists
if [ ! -f "bin/program/JWrapper.class" ]; then
    echo "❌ Main class not compiled. Run:"
    echo "   find src -name '*.java' -exec javac -cp 'src:.' -d bin {} +"
    exit 1
fi

echo "🚀 Running BrickGraphics from source code..."
echo "📁 Using compiled files in: bin/"
echo ""

# Automatically open the default KMV if no arguments are provided
DEFAULT_KMV="lddmc.kvm"
if [ $# -eq 0 ] && [ -f "$DEFAULT_KMV" ]; then
    echo "📄 Opening default mosaic: $DEFAULT_KMV"
    set -- "$DEFAULT_KMV"
fi

# Launch compiled source code
java -cp ".:bin" program.JWrapper "$@"