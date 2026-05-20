#!/usr/bin/env bash
# Spin up all 4 microservices for local development.
#
# Usage:
#   ./run-all.sh start      # start all 4 servers in the background
#   ./run-all.sh stop       # stop them
#   ./run-all.sh restart    # stop + start
#   ./run-all.sh status     # show which are running
#   ./run-all.sh logs [svc] # tail logs (all, or one of: analytics property purchasers gateway)
#   ./run-all.sh build      # mvn package
#
# Logs are written to ./logs/<svc>.log, PIDs to ./.pids/<svc>.pid

set -euo pipefail

cd "$(dirname "$0")"

SERVICES=(analytics property purchasers gateway)

jar_for() {
  case "$1" in
    analytics)  echo "analytics-server/target/analytics-server.jar" ;;
    property)   echo "property-server/target/property-server.jar" ;;
    purchasers) echo "purchasers-server/target/purchasers-server.jar" ;;
    gateway)    echo "api-gateway/target/api-gateway.jar" ;;
  esac
}

port_for() {
  case "$1" in
    analytics)  echo 7073 ;;
    property)   echo 7071 ;;
    purchasers) echo 7072 ;;
    gateway)    echo 7070 ;;
  esac
}

ensure_dirs() { mkdir -p logs .pids; }

build() {
  echo ">>> mvn clean package -DskipTests"
  mvn -q clean package -DskipTests
}

is_running() {
  local svc="$1"
  local pidfile=".pids/$svc.pid"
  [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null
}

check_postgres() {
  if ! nc -z localhost 5432 2>/dev/null; then
    echo "!!! Postgres not reachable on localhost:5432"
    echo "    Run:  docker compose up -d db"
    echo "    (Continuing anyway — gateway can start without it, others will retry on first request.)"
    echo ""
  fi
}

start_one() {
  local svc="$1"
  local jar; jar=$(jar_for "$svc")
  local port; port=$(port_for "$svc")
  local pidfile=".pids/$svc.pid"
  local logfile="logs/$svc.log"

  if is_running "$svc"; then
    echo "    $svc already running (pid $(cat "$pidfile"), port $port)"
    return
  fi

  if [[ ! -f "$jar" ]]; then
    echo "!!! Missing $jar — run: $0 build"
    exit 1
  fi

  nohup java -jar "$jar" > "$logfile" 2>&1 &
  echo $! > "$pidfile"
  echo "    $svc started (pid $(cat "$pidfile"), port $port, log $logfile)"
}

stop_one() {
  local svc="$1"
  local pidfile=".pids/$svc.pid"
  if [[ ! -f "$pidfile" ]]; then
    echo "    $svc not tracked"
    return
  fi
  local pid; pid=$(cat "$pidfile")
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    # wait up to 5s for graceful shutdown
    for _ in 1 2 3 4 5; do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$pid" 2>/dev/null || true
    echo "    $svc stopped (pid $pid)"
  else
    echo "    $svc was not running"
  fi
  rm -f "$pidfile"
}

status() {
  printf "%-12s %-6s %-8s %s\n" SERVICE PORT STATUS PID
  for svc in "${SERVICES[@]}"; do
    local pidfile=".pids/$svc.pid"
    local port; port=$(port_for "$svc")
    if is_running "$svc"; then
      printf "%-12s %-6s %-8s %s\n" "$svc" "$port" "running" "$(cat "$pidfile")"
    else
      printf "%-12s %-6s %-8s %s\n" "$svc" "$port" "stopped" "-"
    fi
  done
}

cmd_start() {
  ensure_dirs
  check_postgres
  # Start the 3 backends first, then the gateway
  echo ">>> starting backends"
  for svc in analytics property purchasers; do
    start_one "$svc"
  done
  # Tiny pause so the gateway's startup log shows reachable backends
  sleep 1
  echo ">>> starting gateway"
  start_one gateway
  echo ""
  status
  echo ""
  echo "Public entry point: http://localhost:7070/"
  echo "Tail logs:          $0 logs [analytics|property|purchasers|gateway]"
}

cmd_stop() {
  echo ">>> stopping all"
  # Reverse order: gateway first, then backends
  for svc in gateway purchasers property analytics; do
    stop_one "$svc"
  done
}

cmd_logs() {
  local target="${1:-all}"
  if [[ "$target" == "all" ]]; then
    tail -F logs/*.log
  else
    tail -F "logs/$target.log"
  fi
}

case "${1:-}" in
  start)   cmd_start ;;
  stop)    cmd_stop ;;
  restart) cmd_stop; cmd_start ;;
  status)  status ;;
  logs)    cmd_logs "${2:-all}" ;;
  build)   build ;;
  *)
    echo "Usage: $0 {start|stop|restart|status|logs [svc]|build}"
    exit 1
    ;;
esac