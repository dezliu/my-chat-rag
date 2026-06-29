FROM node:20-alpine AS builder

WORKDIR /app

COPY chat-h5/package.json ./
RUN npm install

COPY chat-h5/ ./

ENV API_URL=http://server:8080
RUN npm run build

FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV API_URL=http://server:8080
ENV PORT=3001
ENV HOSTNAME=0.0.0.0

RUN addgroup -S nextjs && adduser -S nextjs -G nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nextjs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3001

CMD ["node", "server.js"]
