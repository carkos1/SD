#!/bin/bash
# run.sh - A script to start the RMI registry and BarrelServer

# Define the port and jar file (adjust if necessary)
RMI_PORT=7777
JAR_FILE="target/sd-tutorial2-1.0-SNAPSHOT.jar"
MAIN_CLASS="dei.uc.pt.search.BarrelServer"  # Adjust based on your package structure

# Start the RMI registry in the background
echo "Starting rmiregistry on port ${RMI_PORT}..."
rmiregistry ${RMI_PORT} &
REGISTRY_PID=$!

# Wait for a few seconds to ensure the registry is running
sleep 2

# Start the BarrelServer
echo "Starting BarrelServer..."
java -cp ${JAR_FILE} ${MAIN_CLASS}

# Optionally, kill the RMI registry when done (if desired)
echo "Stopping rmiregistry..."
kill ${REGISTRY_PID}
