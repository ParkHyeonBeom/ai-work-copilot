#!/bin/bash
set -e

echo "=== Building all modules ==="
mvn clean package -DskipTests

echo "=== Starting Docker Compose ==="
docker compose up --build
