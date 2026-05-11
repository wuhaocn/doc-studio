#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_H2_RUNTIME_DB_FILE="$PROJECT_ROOT/var/memora-runtime/memora_doc"

should_prepare_default_h2_runtime() {
    local active_profiles=",${SPRING_PROFILES_ACTIVE:-},"
    case "$active_profiles" in
        *,dev,*|*,test,*|*,mysql,*)
            return 1
            ;;
        *)
            return 0
            ;;
    esac
}

resolve_runtime_db_file() {
    local db_file="${MEMORA_DB_FILE:-}"
    if [ -z "$db_file" ]; then
        printf '%s\n' "$DEFAULT_H2_RUNTIME_DB_FILE"
        return 0
    fi

    case "$db_file" in
        /*)
            printf '%s\n' "$db_file"
            ;;
        "~/"*)
            printf '%s\n' "$HOME/${db_file#~/}"
            ;;
        *)
            printf '%s\n' "$PROJECT_ROOT/${db_file#./}"
            ;;
    esac
}

prepare_runtime_storage() {
    if ! should_prepare_default_h2_runtime; then
        return 0
    fi

    MEMORA_DB_FILE="$(resolve_runtime_db_file)"
    export MEMORA_DB_FILE
    mkdir -p "$(dirname "$MEMORA_DB_FILE")"
    echo "默认运行数据库: $MEMORA_DB_FILE"
}

prepare_runtime_storage

if [ -f "$PROJECT_ROOT/scripts/lib/gradle-env.sh" ]; then
    # shellcheck source=scripts/lib/gradle-env.sh
    source "$PROJECT_ROOT/scripts/lib/gradle-env.sh"
    memora_exec_gradle :memora-server-start:bootRun "$@"
    exit 0
fi

JAR_PATH="$(find "$PROJECT_ROOT" -maxdepth 1 -name '*.jar' | head -n 1)"
if [ -z "$JAR_PATH" ]; then
    echo "错误：当前目录未找到可启动的后端 jar。"
    echo "仓库开发态请在项目根目录执行 ./start-backend.sh，发布态请确认同目录下存在构建产物 jar。"
    exit 1
fi

echo "使用 jar 启动后端: $JAR_PATH"
exec java -jar "$JAR_PATH" "$@"
