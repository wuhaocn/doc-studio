#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

if [ -n "${MEMORA_GRADLE_CMD:-}" ]; then
    GRADLE_CMD=("$MEMORA_GRADLE_CMD")
elif command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD=("gradle")
elif [ -x "$PROJECT_ROOT/gradlew" ]; then
    GRADLE_CMD=("$PROJECT_ROOT/gradlew")
else
    echo "错误：未找到可用的 Gradle 命令。"
    echo "请安装系统 Gradle，或确保仓库内的 ./gradlew 可用后重试。"
    exit 1
fi

echo "项目目录: $PROJECT_ROOT"
echo "启动命令: ${GRADLE_CMD[*]} :memora-server-start:bootRun"

cd "$PROJECT_ROOT"
"${GRADLE_CMD[@]}" :memora-server-start:bootRun
