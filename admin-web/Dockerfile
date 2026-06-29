# syntax=docker/dockerfile:1

FROM node:20-alpine AS builder

WORKDIR /app

COPY admin-web/package.json ./
RUN npm install

COPY admin-web/ ./
RUN npm run build

FROM nginx:1.27-alpine

COPY admin-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
