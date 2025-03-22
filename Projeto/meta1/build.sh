#!/bin/bash
# build.sh - Compile all Java files recursively

# Path to the JSoup jar (adjust if needed)
JSOUP_JAR="./libs/jsoup-1.19.1.jar"

# Check if the required library exists
if [ ! -f "$JSOUP_JAR" ]; then
  echo "Error: $JSOUP_JAR not found. Please download it from https://jsoup.org/download and place it in the current directory."
  exit 1
fi

# Find all Java source files and save the list to sources.txt
echo "Finding Java source files..."
find . -name "*.java" > sources.txt

# Compile all Java files using the JSoup library on the classpath
echo "Compiling Java files..."
javac -cp .:"$JSOUP_JAR" @sources.txt

if [ $? -eq 0 ]; then
  echo "Compilation successful!"
else
  echo "Compilation failed."
fi

# Clean up the temporary sources.txt file
rm sources.txt
