@echo off
REM run.bat - Inicia todos os serviços

set RMI_PORT=8183
set JAR_FILE=target\sd-tutorial2.jar

REM Mata processos antigos
taskkill /F /IM rmiregistry.exe /T > nul 2>&1
taskkill /F /IM java.exe /T > nul 2>&1
timeout /t 1 /nobreak > nul

REM Inicia o RMI Registry com classpath
echo Iniciando RMI Registry na porta %RMI_PORT%...
start "RMI Registry" rmiregistry -J-Djava.class.path=%JAR_FILE% %RMI_PORT%
timeout /t 3 /nobreak > nul

REM Inicia Barrels
echo Iniciando Barrels...
start "Barrel 1" java -cp %JAR_FILE% search.BarrelServer barrel1
timeout /t 2 /nobreak > nul
start "Barrel 2" java -cp %JAR_FILE% search.BarrelServer barrel2
timeout /t 5 /nobreak > nul

REM Inicia Downloader
echo Iniciando Downloader...
start "Downloader" java -cp %JAR_FILE% search.Downloader localhost %RMI_PORT%
timeout /t 2 /nobreak > nul

REM Inicia Gateway (bloqueante)
echo Iniciando Gateway...
java -cp %JAR_FILE% search.Gateway

REM Encerra processos após o Gateway terminar
taskkill /F /IM rmiregistry.exe /T > nul 2>&1
