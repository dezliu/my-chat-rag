ARG DOCKER_IMAGE_PREFIX=
FROM ${DOCKER_IMAGE_PREFIX}node:20-alpine AS builder

WORKDIR /app
COPY graphql-bff/package.json ./
RUN npm install

COPY graphql-bff/ ./
RUN npm run build

ARG DOCKER_IMAGE_PREFIX=
FROM ${DOCKER_IMAGE_PREFIX}node:20-alpine

WORKDIR /app
ENV NODE_ENV=production
COPY graphql-bff/package.json ./
RUN npm install --omit=dev
COPY --from=builder /app/dist ./dist

EXPOSE 4000
CMD ["node", "dist/index.js"]
