@echo off
REM build.bat - Compila o projeto com Maven

mvn clean package

if %errorlevel% equ 0 (
    echo ----------------------------------------------------
    echo Build bem-sucedido! JAR gerado em: target\sd-tutorial2.jar
    echo ----------------------------------------------------
) else (
    echo Erro durante o build. Verifique o código.
    exit /b 1
)
