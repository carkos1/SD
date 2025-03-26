#!/bin/bash
# build.sh - Compila o projeto com Maven

mvn clean package

if [ $? -eq 0 ]; then
    echo "----------------------------------------------------"
    echo "Build bem-sucedido! JAR gerado em: target/sd-tutorial2.jar"
    echo "----------------------------------------------------"
else
    echo "Erro durante o build. Verifique o código."
    exit 1
fi