# --- 1. asama: React uygulamasini derle ---
FROM node:20-alpine AS build
WORKDIR /app

# package*.json once -> npm ci katmani cache'lenir, kaynak degisince tekrar kurulmaz.
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build   # cikti: /app/dist (statik dosyalar)

# --- 2. asama: Nginx ile statik dosyalari sun ---
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
