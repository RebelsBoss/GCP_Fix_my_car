#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="YOUR_PROJECT_ID"
REGION="YOUR_REGION"
REPO="YOUR_ARTIFACT_REPO"

FRONTEND_IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/frontend-vertex-ai-search:latest"
BACKEND_IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/backend-vertex-ai-search:latest"

echo "🔐 Configuring Docker auth for Artifact Registry..."
gcloud auth configure-docker "${REGION}-docker.pkg.dev" -q

echo "🟥 BUILDING AND PUSHING STREAMLIT FRONTEND..."
docker build -t "${FRONTEND_IMAGE}" -f frontend/Dockerfile frontend
docker push "${FRONTEND_IMAGE}"

echo "☕ BUILDING AND PUSHING JAVA BACKEND..."
docker build -t "${BACKEND_IMAGE}" -f backend/Dockerfile backend
docker push "${BACKEND_IMAGE}"

echo "✅ Container build and push complete."
