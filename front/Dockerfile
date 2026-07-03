# syntax=docker/dockerfile:1

FROM node:20-alpine AS base
RUN apk add --no-cache libc6-compat curl
WORKDIR /repo

FROM base AS deps
COPY package.json package-lock.json ./
COPY front/package.json ./front/
RUN npm ci -w front

FROM base AS builder
COPY package.json package-lock.json ./
COPY front ./front
WORKDIR /repo/front
ARG NEXT_PUBLIC_BASE_PATH=/allohub
ARG NEXT_PUBLIC_API_BASE=/allohub/api
ARG BACKEND_URL=http://back:8080
ENV NEXT_TELEMETRY_DISABLED=1
ENV NEXT_PUBLIC_BASE_PATH=$NEXT_PUBLIC_BASE_PATH
ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE
ENV BACKEND_URL=$BACKEND_URL
RUN npm run build

FROM base AS runner
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

ARG NEXT_PUBLIC_BASE_PATH=/allohub
ARG NEXT_PUBLIC_API_BASE=/allohub/api
ENV NEXT_PUBLIC_BASE_PATH=$NEXT_PUBLIC_BASE_PATH
ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE

RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

WORKDIR /app
COPY --from=builder --chown=nextjs:nodejs /repo/front/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /repo/front/.next/static ./front/.next/static
COPY --from=builder /repo/front/public ./front/public

USER nextjs
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:3000/allohub/ || exit 1

CMD ["node", "front/server.js"]
