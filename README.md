# brickgraphics

Realizing the website that is hosting this project is not doing so well right now, this is how to install and run locally:

## Install JAVA SDK

Use https://openjdk.org/ or similar to get the command line tools "javac" and "java"

## Compile

javac -cp src -d bin src/mosaic/controllers/MainController.java

## Run

java -cp bin mosaic.controllers.MainController

En Windows, después de generar el paquete distribuible, se puede abrir la app con doble clic usando `BrickGraphics_Windows.bat`, que ejecuta `java -jar BrickGraphics.jar` y mantiene abierta la consola para mostrar errores.

## Crear un paquete distribuible

Para generar una carpeta limpia y un ZIP listos para compartir, ejecuta:

```
./package_distribution.sh
```

El script recompila el proyecto, copia los binarios y recursos necesarios dentro de `dist/BrickGraphics_<fecha>` y crea `dist/BrickGraphics_<fecha>.zip` para enviar a otros usuarios.
