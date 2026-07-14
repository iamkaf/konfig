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
test_file="test/teakit/title-config.test.ts"

workspace_root="$(git rev-parse --show-superproject-working-tree 2>/dev/null || true)"
catalog_root="${KONFIG_VERSION_CATALOG_ROOT:-}"
if [ -z "$catalog_root" ] && [ -n "$workspace_root" ]; then
  catalog_root="$workspace_root/tooling/version-catalog"
fi
catalog="$catalog_root/mc-$version/gradle/libs.versions.toml"
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

if [ "$loader" = "forge" ] || [ "$loader" = "neoforge" ]; then
  unset WAYLAND_DISPLAY
  export XDG_SESSION_TYPE=x11
  export GLFW_PLATFORM=x11
fi

./teakitw run \
  --node "$node" \
  --test-file "$test_file" \
  --timeout "$timeout_seconds"
