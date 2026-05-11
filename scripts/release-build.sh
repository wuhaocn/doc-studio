#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RELEASE_DIR="$PROJECT_ROOT/release"
BACKEND_RELEASE_DIR="$RELEASE_DIR/backend"
WEB_RELEASE_DIR="$RELEASE_DIR/web"
BUILD_INFO_FILE="$RELEASE_DIR/BUILD_INFO.txt"
RELEASE_README_FILE="$RELEASE_DIR/README.md"

resolve_backend_jar_path() {
    find "$PROJECT_ROOT/memora-server/memora-server-start/build/libs" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1
}

resolve_git_commit() {
    if command -v git >/dev/null 2>&1 && git -C "$PROJECT_ROOT" rev-parse --short HEAD >/dev/null 2>&1; then
        git -C "$PROJECT_ROOT" rev-parse --short HEAD
        return 0
    fi
    printf '%s\n' "unknown"
}

resolve_git_tree_state() {
    if command -v git >/dev/null 2>&1 && git -C "$PROJECT_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
        if [ -n "$(git -C "$PROJECT_ROOT" status --short)" ]; then
            printf '%s\n' "dirty"
        else
            printf '%s\n' "clean"
        fi
        return 0
    fi
    printf '%s\n' "unknown"
}

write_build_info() {
    local build_time
    local git_commit
    local git_tree_state
    local backend_jar_name

    build_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
    git_commit="$(resolve_git_commit)"
    git_tree_state="$(resolve_git_tree_state)"
    backend_jar_name="$(basename "$(find "$BACKEND_RELEASE_DIR" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)")"

    cat > "$BUILD_INFO_FILE" <<EOF
build_time=$build_time
git_commit=$git_commit
git_tree_state=$git_tree_state
backend_jar=$backend_jar_name
web_dir=web
EOF
}

write_release_readme() {
    local backend_jar_name
    backend_jar_name="$(basename "$(find "$BACKEND_RELEASE_DIR" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)")"

    cat > "$RELEASE_README_FILE" <<EOF
# Release Package

本目录由 \`./scripts/release-build.sh\` 自动生成。

## 目录结构

\`\`\`text
release/
├── BUILD_INFO.txt
├── README.md
├── backend/
│   ├── $backend_jar_name
│   ├── application.yml
│   └── start-backend.sh
└── web/
\`\`\`

## Backend 启动

\`\`\`bash
cd backend
./start-backend.sh
\`\`\`

常用环境变量：

- \`MEMORA_DB_FILE\`：覆盖默认 H2 运行文件路径
- \`MEMORA_WEB_ALLOWED_ORIGINS\`：允许的 Web Origin
- \`MEMORA_AUTH_SESSION_COOKIE_SECURE\`：HTTPS 环境建议设为 \`true\`
- \`SPRING_PROFILES_ACTIVE\`：如需切换 profile 再显式设置

说明：

- \`var/\` 下的运行态数据库文件属于本地运行数据，不应提交回仓库。
- 当前浏览器会话默认依赖 \`HttpOnly Cookie\`，上线时需要同时确认 CORS Origin 和 Cookie 策略。

## Web 部署

\`web/\` 为静态前端产物目录，可交给任意静态资源服务器托管。

## 发布前建议

- Backend：\`./scripts/backend-test.sh\`
- Web：\`cd memora-web-app && npm run lint && npm run test:unit && npm run build\`
- 人工冒烟：登录、工作区切换、邀请接受、知识库/文档、受控分享、API key
EOF
}

"$PROJECT_ROOT/scripts/backend-build.sh"

cd "$PROJECT_ROOT/memora-web-app"
npm run build

rm -rf "$RELEASE_DIR"
mkdir -p "$BACKEND_RELEASE_DIR" "$WEB_RELEASE_DIR"

BACKEND_JAR_PATH="$(resolve_backend_jar_path)"
if [ -z "$BACKEND_JAR_PATH" ]; then
    echo "错误：未找到可发布的后端 boot jar。"
    exit 1
fi

cp "$BACKEND_JAR_PATH" "$BACKEND_RELEASE_DIR/"
cp "$PROJECT_ROOT/start-backend.sh" "$BACKEND_RELEASE_DIR/"
cp "$PROJECT_ROOT/memora-server/memora-server-start/src/main/resources/application.yml" "$BACKEND_RELEASE_DIR/"
cp -R "$PROJECT_ROOT/memora-web-app/dist/." "$WEB_RELEASE_DIR/"
write_build_info
write_release_readme

echo "发布产物目录: $RELEASE_DIR"
echo "后端产物: $BACKEND_RELEASE_DIR"
echo "前端产物: $WEB_RELEASE_DIR"
