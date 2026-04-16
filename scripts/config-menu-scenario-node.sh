#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Usage: $0 <node> [timeout-seconds]" >&2
  exit 1
fi

node="$1"
timeout_seconds="${2:-240}"

cd "$(dirname "$0")/.."

if ! just list-nodes | grep -Fxq "$node"; then
  echo "Unknown node: $node" >&2
  exit 1
fi

version="${node%-*}"
loader="${node##*-}"
gradle_task=":$loader:$version:runClient"
if [ "$version" = "1.16.5" ] && [ "$loader" = "forge" ]; then
  gradle_task=":forge:1.16.5:runLegacyClient"
fi

case "$loader" in
  fabric)
    scenario_file="test/scenarios/konfig/title-config-fabric.json"
    case "$version" in
      1.14.4|1.15|1.15.1|1.15.2|1.16|1.16.1|1.16.2|1.16.3|1.16.4|1.16.5)
        scenario_file="test/scenarios/konfig/title-config-fabric-legacy.json"
        ;;
      1.17|1.17.1|1.18|1.18.1|1.18.2|1.19|1.19.1|1.19.2)
        scenario_file="test/scenarios/konfig/title-config-fabric-117.json"
        ;;
      1.19.3|1.19.4)
        scenario_file="test/scenarios/konfig/title-config-fabric-11934.json"
        ;;
      1.20|1.20.1|1.20.2)
        scenario_file="test/scenarios/konfig/title-config-fabric-11934.json"
        ;;
      1.20.3|1.20.4)
        scenario_file="test/scenarios/konfig/title-config-fabric-12034.json"
        ;;
      1.21.2)
        scenario_file="test/scenarios/konfig/title-config-fabric-12034.json"
        ;;
    esac
    instance_path="fabric/versions/$version/runs/client/teakit/instance.json"
    ;;
  forge)
    scenario_file="test/scenarios/konfig/title-config-forge.json"
    if [ "$version" = "1.16.5" ]; then
      scenario_file="test/scenarios/konfig/title-config-forge-legacy.json"
    fi
    instance_path="forge/versions/$version/run/teakit/instance.json"
    ;;
  neoforge)
    scenario_file="test/scenarios/konfig/title-config-neoforge.json"
    instance_path="neoforge/versions/$version/run/teakit/instance.json"
    ;;
  *)
    echo "Unsupported loader: $loader" >&2
    exit 1
    ;;
esac

catalog="/home/kaf/code/mods/version-catalog/mc-$version/gradle/libs.versions.toml"
if [ ! -f "$catalog" ] || ! rg -q '^teakit = ' "$catalog"; then
  echo "TeaKit is not configured in the shared catalog for $version" >&2
  exit 1
fi

log="/tmp/konfig-$node.config-menu.run.log"
result="/tmp/konfig-$node.config-menu.result.json"
health="/tmp/konfig-$node.config-menu.health.json"
probe="/tmp/konfig-$node.config-menu.probe.json"
rm -f "$instance_path" "$log" "$result" "$health" "$probe"

if [ "$loader" = "fabric" ]; then
  rm -rf \
    ".gradle/loom-cache/remapped_mods/remapped/com/iamkaf/teakit" \
    ".gradle/loom-cache/remapped_mods/remapped/com/terraformersmc" \
    ".gradle/loom-cache/remapped_mods/remapped/maven/modrinth"
  find "fabric/versions/$version/build/loom-cache/remapped_working" \
    -maxdepth 1 \
    \( -iname '*teakit*' -o -iname '*modmenu*' -o -iname '*resource-loader*' \) \
    -exec rm -rf {} + 2>/dev/null || true
fi

./gradlew --configure-on-demand --refresh-dependencies "$gradle_task" --console=plain \
  -Dkonfig.withTeaKit=true \
  -Dteakit.autoExitTitle=false \
  -Dteakit.repoRoot="$PWD" \
  -Dteakit.scenarioRoot="$PWD" \
  >"$log" 2>&1 &
gradle_pid=$!

port=""
token=""
base_url=""

cleanup() {
  set +e
  if [ -f "$instance_path" ]; then
    port="$(jq -r '.port' "$instance_path" 2>/dev/null || true)"
    token="$(jq -r '.token' "$instance_path" 2>/dev/null || true)"
    if [ -n "$port" ] && [ -n "$token" ] && [ "$port" != "null" ] && [ "$token" != "null" ]; then
      base_url="http://localhost:$port"
      curl -fsS -H "X-TeaKit-Token: $token" \
        -H 'Content-Type: application/json' \
        --data '{"delayMs":500}' \
        "$base_url/action/client/quit" >/dev/null 2>&1 || true
    fi
  fi
  if kill -0 "$gradle_pid" >/dev/null 2>&1; then
    wait "$gradle_pid" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

for _ in $(seq 1 "$timeout_seconds"); do
  if [ -f "$instance_path" ]; then
    port="$(jq -r '.port' "$instance_path" 2>/dev/null || true)"
    token="$(jq -r '.token' "$instance_path" 2>/dev/null || true)"
    base_url="http://localhost:$port"
    if [ -n "$port" ] && [ -n "$token" ] && [ "$port" != "null" ] && [ "$token" != "null" ] \
      && curl -fsS -H "X-TeaKit-Token: $token" "$base_url/health" >"$health" 2>/dev/null; then
      break
    fi
  fi
  if ! kill -0 "$gradle_pid" >/dev/null 2>&1; then
    wait "$gradle_pid"
    tail -n 160 "$log" >&2
    exit 1
  fi
  sleep 1
done

if [ ! -f "$health" ]; then
  tail -n 160 "$log" >&2
  exit 1
fi

screen_ready=0
for _ in $(seq 1 "$timeout_seconds"); do
  if curl -fsS -H "X-TeaKit-Token: $token" "$base_url/probe/menu" >"$probe" 2>/dev/null \
    && jq -e '
      if .open == false then true
      else (.title // "") != "Loading Minecraft"
      end
    ' "$probe" >/dev/null 2>&1; then
    screen_ready=1
    break
  fi
  if ! kill -0 "$gradle_pid" >/dev/null 2>&1; then
    wait "$gradle_pid"
    tail -n 160 "$log" >&2
    exit 1
  fi
  sleep 1
done

if [ "$screen_ready" -ne 1 ]; then
  cat "$probe" >&2 || true
  tail -n 160 "$log" >&2
  exit 1
fi

# Give the title screen a moment to settle before firing UI automation.
sleep 2

http_code="$(
  curl -sS -o "$result" -w '%{http_code}' \
    --max-time "$((timeout_seconds + 30))" \
    -H "X-TeaKit-Token: $token" \
    -H 'Content-Type: application/json' \
    --data-binary @"$scenario_file" \
    "$base_url/scenario/run"
)"
if [ "$http_code" != "200" ]; then
  cat "$result" >&2 || true
  tail -n 160 "$log" >&2
  exit 1
fi

expected_steps=$(jq '.steps | length' "$scenario_file")
actual_steps=$(jq '.steps | length' "$result")
if [ "$actual_steps" -lt "$expected_steps" ]; then
  cat "$result" >&2 || true
  exit 1
fi

curl -fsS -H "X-TeaKit-Token: $token" \
  -H 'Content-Type: application/json' \
  --data '{"delayMs":500}' \
  "$base_url/action/client/quit" >/dev/null

for i in $(seq 1 15); do
  if ! kill -0 "$gradle_pid" >/dev/null 2>&1; then
    break
  fi
  if [ "$i" -eq 5 ]; then
    curl -fsS -H "X-TeaKit-Token: $token" \
      -H 'Content-Type: application/json' \
      --data '{"delayMs":500}' \
      "$base_url/action/client/quit" >/dev/null 2>&1 || true
  fi
  sleep 1
done

wait "$gradle_pid"
grep -q 'Initializing TeaKit on' "$log"
if ! grep -q 'Konfig initialized' "$log"; then
  jq -e '
    .steps[]?
    | select(.action == "wait_for_screen")
    | .result.screenClass == "com.iamkaf.konfig.impl.v1.KonfigConfigScreen"
      or .result.screenClass == "com.iamkaf.konfig.forge.KonfigConfigScreen"
      or .result.screenClass == "com.iamkaf.konfig.fabric.KonfigConfigScreen"
  ' "$result" >/dev/null
fi
echo "Config menu scenario OK: $node"
