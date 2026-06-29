#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

COMPOSE_FILE="docker-compose.infra.yml"
PID_DIR="$ROOT/.dev/pids"
LOG_DIR="$ROOT/.dev/logs"

usage() {
  cat <<'EOF'
用法: ./scripts/dev-all.sh <up|down|restart|status> [--infra]

  up        启动基础设施 + 后端 + GraphQL BFF + 管理前端 + 用户 H5
  down      停止宿主机应用进程；加 --infra 时同时停止 MySQL/Redis/Qdrant
  restart   重启全部应用（不停止基础设施）
  status    查看基础设施与应用状态

首次使用前请: cp .env.example .env，并在 admin-web、chat-h5、graphql-bff 各执行 npm install
查看日志: tail -f .dev/logs/server.log
EOF
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "错误: 未找到命令 $1" >&2
    exit 1
  fi
}

setup_java() {
  if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]]; then
      export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
      export PATH="$JAVA_HOME/bin:$PATH"
    elif [[ -d /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]]; then
      export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
      export PATH="$JAVA_HOME/bin:$PATH"
    fi
  fi
  if ! java -version 2>&1 | grep -q 'version "21'; then
    echo "警告: 建议使用 Java 21，当前版本:" >&2
    java -version 2>&1 | head -1 >&2 || true
  fi
}

load_env() {
  if [[ ! -f "$ROOT/.env" ]]; then
    echo "错误: 未找到 .env，请先执行 cp .env.example .env 并配置 AI_DASHSCOPE_API_KEY" >&2
    exit 1
  fi
  set -a
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "${line// }" ]] && continue
    export "$line"
  done < "$ROOT/.env"
  set +a
}

mkdir -p "$PID_DIR" "$LOG_DIR"

wait_mysql_healthy() {
  local i
  echo "等待 MySQL 就绪..."
  for i in $(seq 1 30); do
    if docker compose -f "$COMPOSE_FILE" ps mysql 2>/dev/null | grep -q healthy; then
      echo "MySQL 已就绪"
      return 0
    fi
    sleep 2
  done
  echo "错误: MySQL 未在 60s 内变为 healthy" >&2
  docker compose -f "$COMPOSE_FILE" ps
  exit 1
}

wait_graphql_bff() {
  local i
  echo "等待 GraphQL BFF 就绪..."
  for i in $(seq 1 30); do
    if curl -sf http://localhost:4000/graphql \
      -H 'Content-Type: application/json' \
      -d '{"query":"{ health }"}' >/dev/null 2>&1; then
      echo "GraphQL BFF 已就绪 (http://localhost:4000/graphql)"
      return 0
    fi
    sleep 2
  done
  echo "错误: GraphQL BFF 未在 60s 内就绪，查看 .dev/logs/graphql-bff.log" >&2
  exit 1
}

wait_server_healthy() {
  local i
  echo "等待后端就绪..."
  for i in $(seq 1 60); do
    if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
      echo "后端已就绪 (http://localhost:8080)"
      return 0
    fi
    sleep 2
  done
  echo "错误: 后端未在 120s 内就绪，查看 .dev/logs/server.log" >&2
  exit 1
}

is_running() {
  local pid_file="$1"
  [[ -f "$pid_file" ]] || return 1
  local pid
  pid="$(cat "$pid_file")"
  kill -0 "$pid" 2>/dev/null
}

start_process() {
  local name="$1"
  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  if is_running "$pid_file"; then
    echo "$name 已在运行 (PID $(cat "$pid_file"))"
    return 0
  fi

  echo "启动 $name ..."
  case "$name" in
    server)
      (
        cd "$ROOT"
        setup_java
        load_env
        exec mvn -q -pl myrag-server spring-boot:run
      ) >"$log_file" 2>&1 &
      ;;
    admin-web)
      (
        cd "$ROOT/admin-web"
        exec npm run dev
      ) >"$log_file" 2>&1 &
      ;;
    chat-h5)
      (
        cd "$ROOT/chat-h5"
        exec npm run dev
      ) >"$log_file" 2>&1 &
      ;;
    graphql-bff)
      (
        cd "$ROOT/graphql-bff"
        load_env
        export REST_BASE_URL="${REST_BASE_URL:-http://localhost:8080}"
        export PORT="${GRAPHQL_BFF_PORT:-4000}"
        exec npm run dev
      ) >"$log_file" 2>&1 &
      ;;
    *)
      echo "未知进程: $name" >&2
      exit 1
      ;;
  esac
  echo $! >"$pid_file"
  echo "$name 已启动 (PID $(cat "$pid_file"), 日志: $log_file)"
}

stop_pid_file() {
  local name="$1"
  local pid_file="$PID_DIR/$name.pid"
  if [[ ! -f "$pid_file" ]]; then
    return 0
  fi
  local pid
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    echo "停止 $name (PID $pid)..."
    kill "$pid" 2>/dev/null || true
    local i
    for i in $(seq 1 10); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  fi
  rm -f "$pid_file"
}

stop_port() {
  local port="$1"
  local pids
  pids="$(lsof -ti ":$port" 2>/dev/null || true)"
  if [[ -n "$pids" ]]; then
    echo "清理端口 $port 上的残留进程..."
    # shellcheck disable=SC2086
    kill $pids 2>/dev/null || true
    sleep 1
    # shellcheck disable=SC2086
    kill -9 $pids 2>/dev/null || true
  fi
}

cmd_up() {
  require_cmd docker
  require_cmd java
  require_cmd mvn
  require_cmd node
  require_cmd npm
  require_cmd curl
  load_env

  echo "==> 启动基础设施"
  docker compose -f "$COMPOSE_FILE" up -d
  wait_mysql_healthy

  start_process server
  wait_server_healthy
  start_process graphql-bff
  wait_graphql_bff
  start_process admin-web
  start_process chat-h5

  echo ""
  echo "全部服务已启动:"
  echo "  后端 REST:    http://localhost:8080"
  echo "  GraphQL BFF:  http://localhost:4000/graphql"
  echo "  管理后台:     http://localhost:3000"
  echo "  用户 H5:      http://localhost:3001"
  echo "  日志目录:     $LOG_DIR"
}

cmd_down() {
  local with_infra=false
  if [[ "${1:-}" == "--infra" ]]; then
    with_infra=true
  fi

  echo "==> 停止应用进程"
  stop_pid_file server
  stop_pid_file graphql-bff
  stop_pid_file admin-web
  stop_pid_file chat-h5
  stop_port 8080
  stop_port 4000
  stop_port 3000
  stop_port 3001

  if $with_infra; then
    echo "==> 停止基础设施"
    docker compose -f "$COMPOSE_FILE" down
  else
    echo "基础设施仍在运行（MySQL / Redis / Qdrant）"
  fi
  echo "已停止"
}

check_http() {
  local url="$1"
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || echo "000")"
  if [[ "$code" == "200" ]] || [[ "$code" == "302" ]]; then
    echo "OK ($code)"
  else
    echo "FAIL ($code)"
  fi
}

cmd_status() {
  echo "==> 基础设施"
  docker compose -f "$COMPOSE_FILE" ps 2>/dev/null || echo "(docker compose 不可用)"

  echo ""
  echo "==> 应用进程"
  for name in server graphql-bff admin-web chat-h5; do
    local pid_file="$PID_DIR/$name.pid"
    if is_running "$pid_file"; then
      echo "  $name: running (PID $(cat "$pid_file"))"
    else
      echo "  $name: stopped"
    fi
  done

  echo ""
  echo "==> HTTP 探测"
  echo "  http://localhost:8080/actuator/health -> $(check_http http://localhost:8080/actuator/health)"
  echo "  http://localhost:4000/graphql (health) -> $(curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:4000/graphql -H 'Content-Type: application/json' -d '{\"query\":\"{ health }\"}' 2>/dev/null || echo 000)"
  echo "  http://localhost:3000/                -> $(check_http http://localhost:3000/)"
  echo "  http://localhost:3001/                -> $(check_http http://localhost:3001/)"
}

CMD="${1:-}"
shift || true

case "$CMD" in
  up)
    cmd_up
    ;;
  down)
    cmd_down "${1:-}"
    ;;
  restart)
    cmd_down
    cmd_up
    ;;
  status)
    cmd_status
    ;;
  -h|--help|help|"")
    usage
    ;;
  *)
    echo "未知子命令: $CMD" >&2
    usage
    exit 1
    ;;
esac
