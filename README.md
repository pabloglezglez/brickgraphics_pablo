# brickgraphics

Realizing the website that is hosting this project is not doing so well right now, this is how to install and run locally:

## Install JAVA SDK

Use https://openjdk.org/ or similar to get the command line tools "javac" and "java"

## Compile

javac -cp src -d bin src/mosaic/controllers/MainController.java

## Run

java -cp bin mosaic.controllers.MainController

On Windows, after generating the distributable package, you can open the app with a double-click using `BrickGraphics_Windows.bat`. The script runs `java -jar BrickGraphics.jar` and keeps the console open to display any errors.

## Create a distributable package

To create a clean folder and a ZIP ready to share, run:

```
./package_distribution.sh
```

The script recompiles the project, copies the required binaries and resources into `dist/BrickGraphics_<date>`, and generates `dist/BrickGraphics_<date>.zip` that you can send to other users.
