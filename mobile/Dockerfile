FROM node:22-alpine AS deps

WORKDIR /app
RUN corepack enable

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY mobile/package.json ./mobile/package.json
RUN pnpm install --frozen-lockfile --filter @bdis/mobile...

FROM node:22-alpine AS builder

WORKDIR /app
RUN corepack enable

ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY mobile ./mobile
COPY --from=deps /app/node_modules ./node_modules
COPY --from=deps /app/mobile/node_modules ./mobile/node_modules

RUN pnpm --filter @bdis/mobile build:h5

FROM nginx:1.27-alpine AS runner

COPY deploy/nginx/mobile.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/mobile/dist/build/h5 /usr/share/nginx/html

EXPOSE 80
