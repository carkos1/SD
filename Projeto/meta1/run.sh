#!/bin/bash

# Configurações
RMI_PORT=8183
JAR_FILE="target/sd-tutorial2.jar"

# Mata processos antigos
pkill -f "rmiregistry"
pkill -f "search.BarrelServer"
pkill -f "search.Gateway"
pkill -f "search.Downloader"
sleep 1

# Inicia o RMI Registry com classpath
echo "Iniciando RMI Registry na porta $RMI_PORT..."
rmiregistry -J-Djava.class.path=$JAR_FILE $RMI_PORT &
sleep 3

# Inicia Barrels
echo "Iniciando Barrels..."
java -cp $JAR_FILE search.BarrelServer barrel1 &
sleep 2
java -cp $JAR_FILE search.BarrelServer barrel2 &
sleep 5

# Inicia Downloader
echo "Iniciando Downloader..."
java -cp $JAR_FILE search.Downloader localhost $RMI_PORT &
sleep 2

# Inicia Gateway
echo "Iniciando Gateway..."
java -cp $JAR_FILE search.Gateway

# Mantém o script em execução até que o Gateway seja encerrado
wait $GATEWAY_PID

# Encerra processos
kill $REGISTRY_PID
