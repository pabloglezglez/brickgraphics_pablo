@echo off
REM Launcher para usuarios de Windows.
REM Ejecuta el JAR y mantiene la ventana abierta para mostrar errores.

cd /d "%~dp0"
java -jar BrickGraphics.jar %*
pause
