#!/bin/bash
set -e

echo "=== spring-perf-lab startup ==="

# --- Frontend dependencies ---
echo "[1/2] Installing frontend npm dependencies..."
cd /workspaces/spring-perf-lab/frontend
npm install --silent

# --- Affiche les URLs Codespace ---
CODESPACE_URL_FRONTEND="https://${CODESPACE_NAME}-5173.app.github.dev"
CODESPACE_URL_BACKEND="https://${CODESPACE_NAME}-8080.app.github.dev"

echo ""
echo "████████████████████████████████████████████████"
echo "  App React  → ${CODESPACE_URL_FRONTEND}"
echo "  API Spring → ${CODESPACE_URL_BACKEND}"
echo "████████████████████████████████████████████████"
echo ""

# --- Lancer le backend en arrière-plan ---
echo "[2/2] Starting Spring Boot backend (port 8080)..."
cd /workspaces/spring-perf-lab/backend
./mvnw spring-boot:run -q &

# --- Lancer le frontend en arrière-plan ---
echo "Starting React frontend (port 5173)..."
cd /workspaces/spring-perf-lab/frontend
npm run dev -- --host 0.0.0.0 &

echo "Both servers started. Check the PORTS tab in VS Code."
