@echo off
rem Builds DooreLauncher.exe (an app folder with its own Java) into out\DooreLauncher.
rem Needs JDK 21+ (javac, jar, jpackage). Put doorevisuals-*.jar next to this file to bundle the mod.
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out\classes out\input
javac -encoding UTF-8 --release 21 -d out\classes src\dev\doorelauncher\*.java || exit /b 1
jar --create --file out\input\DooreLauncher.jar --main-class dev.doorelauncher.Launcher -C out\classes . || exit /b 1
copy /y launcher.json out\input\launcher.json >nul
copy /y news.txt out\input\news.txt >nul
for %%f in (doorevisuals-*.jar) do copy /y "%%f" out\input\ >nul
jpackage --type app-image --name DooreLauncher --input out\input --main-jar DooreLauncher.jar --main-class dev.doorelauncher.Launcher ^
  --dest out --add-modules java.desktop,java.net.http,java.logging --java-options "-Dfile.encoding=UTF-8" || exit /b 1
echo.
echo Done: out\DooreLauncher\DooreLauncher.exe
