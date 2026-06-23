#!/bin/bash

# Exit immediately if a build command fails
set -e

echo "🚀 Starting Global Digital Bank Image Compilation Pipeline..."

# 1. Build Frontend Image
echo "📦 Building gdb-frontend..."
docker build -t gdb-frontend:latest ./frontend

# 2. Build Infrastructure Services
echo "📦 Building gdb-eureka-server..."
docker build -t gdb-eureka-server:latest ./eureka-server

echo "📦 Building gdb-gateway-service..."
docker build -t gdb-gateway-service:latest ./gateway-service

# 3. Build Microservices
SERVICES=(
  "aadhar-service"
  "account-service"
  "auth-service"
  "company-service"
  "payment-gateway-service"
  "transactions-service"
  "users-service"
)

for SERVICE in "${SERVICES[@]}"; do
  echo "📦 Building gdb-$SERVICE..."
  docker build -t "gdb-$SERVICE:latest" "./$SERVICE"
done

echo "✅ All 10 GDB ecosystem Docker images built and tagged successfully!"