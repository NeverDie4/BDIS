FROM node:22-alpine AS deps
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY frontend/package.json ./frontend/package.json
RUN pnpm install --frozen-lockfile --filter @bdis/frontend...

FROM node:22-alpine AS builder
WORKDIR /app
RUN corepack enable
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY frontend ./frontend
COPY --from=deps /app/node_modules ./node_modules
COPY --from=deps /app/frontend/node_modules ./frontend/node_modules
RUN pnpm --filter @bdis/frontend build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
COPY --from=builder /app/frontend/.next/standalone ./
COPY --from=builder /app/frontend/.next/static ./frontend/.next/static
COPY --from=builder /app/frontend/public ./frontend/public
EXPOSE 3000
CMD ["node", "frontend/server.js"]
