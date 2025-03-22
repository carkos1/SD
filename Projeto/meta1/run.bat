@echo off
REM Set variables for RMI port, JAR file, and main class
set RMI_PORT=7777
set JAR_FILE=target\sd-tutorial2-1.0-SNAPSHOT.jar
set MAIN_CLASS=dei.uc.pt.search.BarrelServer

echo Starting rmiregistry on port %RMI_PORT%...
start rmiregistry %RMI_PORT%

REM Wait for a few seconds to ensure the registry is running
timeout /t 2

echo Starting BarrelServer...
java -cp %JAR_FILE% %MAIN_CLASS%


pause
