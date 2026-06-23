# --- Stage 1: Build Stage ---
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy the rest of the frontend source code and build static production assets
COPY . .
RUN npm run build

# --- Stage 2: Production Serving Stage ---
FROM nginx:stable-alpine

# Copy the static production build files to the default Nginx public directory
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port 80 for web traffic
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]