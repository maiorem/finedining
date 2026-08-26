# 프론트엔드는 런타임 컨테이너가 아니다. dist를 빌드해 공유 볼륨에 내놓고 종료한다
# (CLAUDE.md §13.1). nginx와 backend가 이 볼륨을 읽기 전용으로 마운트한다.
FROM node:22-alpine AS build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@9 --activate
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM alpine:3.20
COPY --from=build /app/dist /dist
CMD ["sh", "-c", "cp -r /dist/. /web-dist/ && echo 'frontend dist copied to web-dist volume'"]
