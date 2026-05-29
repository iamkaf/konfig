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
    ;;
  forge)
    scenario_file="test/scenarios/konfig/title-config-forge.json"
    if [ "$version" = "1.16.5" ]; then
      scenario_file="test/scenarios/konfig/title-config-forge-legacy.json"
    fi
    ;;
  neoforge)
    scenario_file="test/scenarios/konfig/title-config-neoforge.json"
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

./teakitw run \
  --node "$node" \
  --scenario "$scenario_file" \
  --readiness title \
  --timeout "$timeout_seconds"
