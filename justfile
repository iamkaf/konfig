set shell := ["bash", "-euo", "pipefail", "-c"]

default:
  @just --list

list-versions:
  @find versions -mindepth 2 -maxdepth 2 -type f -name 'gradle.properties' -printf '%h\n' | xargs -r -n1 basename | sort -V

list-loaders version:
  @grep '^project.enabled-loaders=' "versions/{{version}}/gradle.properties" | head -n1 | cut -d= -f2- | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | sed '/^$/d'

list-nodes:
  @for props in versions/*/gradle.properties; do version=$(basename "$(dirname "$props")"); loaders=$(sed -nE 's/^project\.enabled-loaders=(.*)$/\1/p' "$props" | head -n1); for loader in $(printf '%s\n' "$loaders" | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | sed '/^$/d'); do echo "$version-$loader"; done; done | sort -V

projects:
  @./gradlew projects --console=plain

clean-generated:
  @rm -rf build versions/*/build common/versions fabric/versions forge/versions neoforge/versions

run first="" second="" *rest:
  @if [ -z "{{first}}" ]; then \
    echo "Usage: just run [version] [loader] <gradle args>"; \
    exit 1; \
  fi
  @first="{{first}}"; \
  second="{{second}}"; \
  rest=( {{rest}} ); \
  task_suffix() { \
    local version="$1"; \
    local loader="$2"; \
    local digits loader_cap; \
    digits="${version//./}"; \
    loader_cap="${loader^}"; \
    printf '%s%s' "$digits" "$loader_cap"; \
  }; \
  ensure_loader_enabled() { \
    local version="$1"; \
    local loader="$2"; \
    if ! just list-loaders "$version" | grep -Fxq "$loader"; then \
      echo "Loader $loader is not enabled for $version"; \
      exit 1; \
    fi; \
  }; \
  run_gradle() { \
    local version="$1"; \
    shift; \
    local props="versions/$version/gradle.properties"; \
    local java_version sdkman_dir best_match java_home use_configure_on_demand task_arg; \
    if [ ! -f "$props" ]; then \
      echo "Version $version not found."; \
      exit 1; \
    fi; \
    java_version=$(sed -nE 's/^project\.build-java=([0-9]+).*/\1/p' "$props" | head -n1); \
    if [ -z "$java_version" ]; then \
      java_version=$(sed -nE 's/^project\.java=([0-9]+).*/\1/p' "$props" | head -n1); \
    fi; \
    sdkman_dir="$HOME/.sdkman/candidates/java"; \
    if [ -n "$java_version" ] && [ -d "$sdkman_dir" ]; then \
      best_match=$(find "$sdkman_dir" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | grep -E "^${java_version}(\\.|-)" | sort -V | tail -n1 || true); \
      if [ -n "$best_match" ]; then \
        java_home="$sdkman_dir/$best_match"; \
        export JAVA_HOME="$java_home"; \
        export PATH="$JAVA_HOME/bin:$PATH"; \
      fi; \
    fi; \
    use_configure_on_demand=1; \
    for task_arg in "$@"; do \
      if [[ "$task_arg" != :* && "$task_arg" != -* ]]; then \
        use_configure_on_demand=0; \
        break; \
      fi; \
    done; \
    if [ "$use_configure_on_demand" -eq 1 ]; then \
      ./gradlew --configure-on-demand "$@" --console=plain; \
    else \
      ./gradlew "$@" --console=plain; \
    fi; \
  }; \
  if [ -d "versions/$first" ]; then \
    version="$first"; \
    if [ -z "$second" ]; then \
      echo "Usage: just run <version> [loader] <gradle args>"; \
      exit 1; \
    fi; \
    tasks=(); \
    if printf '%s\n' fabric forge neoforge | grep -Fxq "$second"; then \
      loader="$second"; \
      ensure_loader_enabled "$version" "$loader"; \
      if [ "${#rest[@]}" -eq 0 ]; then \
        echo "Usage: just run <version> <loader> <gradle args>"; \
        exit 1; \
      fi; \
      task="${rest[0]}"; \
      extra=( "${rest[@]:1}" ); \
      case "$task" in \
        publish) \
          tasks=( ":$loader:$version:publishAllPublicationsToKafMavenRepository" "${extra[@]}" ); \
          ;; \
        runClient) \
          if [ "$version" = "1.16.5" ] && [ "$loader" = "forge" ]; then \
            tasks=( ":forge:1.16.5:runLegacyClient" "${extra[@]}" ); \
          else \
            tasks=( ":$loader:$version:runClient" "${extra[@]}" ); \
          fi; \
          ;; \
        publishMod|publishRelease) \
          suffix=$(task_suffix "$version" "$loader"); \
          tasks=( "publishCurseforge$suffix" "publishModrinth$suffix" "${extra[@]}" ); \
          ;; \
        publishModrinth) \
          tasks=( "publishModrinth$(task_suffix "$version" "$loader")" "${extra[@]}" ); \
          ;; \
        publishCurseforge) \
          tasks=( "publishCurseforge$(task_suffix "$version" "$loader")" "${extra[@]}" ); \
          ;; \
        *) \
          if [[ "$task" == :* ]]; then \
            tasks=( "$task" "${extra[@]}" ); \
          else \
            tasks=( ":$loader:$version:$task" "${extra[@]}" ); \
          fi; \
          ;; \
      esac; \
      run_gradle "$version" "${tasks[@]}"; \
    else \
      task="$second"; \
      extra=( "${rest[@]}" ); \
      case "$task" in \
        downloadTranslations) \
          tasks=( "downloadTranslations" "${extra[@]}" ); \
          ;; \
        runClient|runServer) \
          echo "$task requires a loader: just run <version> <loader> $task"; \
          exit 1; \
          ;; \
        build) \
          tasks=( ":common:$version:build" ); \
          for loader in $(just list-loaders "$version"); do \
            tasks+=( ":$loader:$version:build" ); \
          done; \
          tasks+=( "${extra[@]}" ); \
          ;; \
        compileJava) \
          tasks=( ":common:$version:compileJava" ); \
          for loader in $(just list-loaders "$version"); do \
            tasks+=( ":$loader:$version:compileJava" ); \
          done; \
          tasks+=( "${extra[@]}" ); \
          ;; \
        publish) \
          tasks=( ":common:$version:publishAllPublicationsToKafMavenRepository" ); \
          for loader in $(just list-loaders "$version"); do \
            tasks+=( ":$loader:$version:publishAllPublicationsToKafMavenRepository" ); \
          done; \
          tasks+=( "${extra[@]}" ); \
          ;; \
        publishMod|publishRelease) \
          for loader in $(just list-loaders "$version"); do \
            suffix=$(task_suffix "$version" "$loader"); \
            tasks+=( "publishCurseforge$suffix" "publishModrinth$suffix" ); \
          done; \
          tasks+=( "${extra[@]}" ); \
          ;; \
        publishModrinth) \
          for loader in $(just list-loaders "$version"); do \
            tasks+=( "publishModrinth$(task_suffix "$version" "$loader")" ); \
          done; \
          tasks+=( "${extra[@]}" ); \
          ;; \
        publishCurseforge) \
          for loader in $(just list-loaders "$version"); do \
            tasks+=( "publishCurseforge$(task_suffix "$version" "$loader")" ); \
          done; \
          tasks+=( "${extra[@]}" ); \
          ;; \
        *) \
          tasks=( "$task" "${extra[@]}" ); \
          ;; \
      esac; \
      run_gradle "$version" "${tasks[@]}"; \
    fi; \
  else \
    tasks=( "$first" ); \
    if [ -n "$second" ]; then \
      tasks+=( "$second" ); \
    fi; \
    tasks+=( "${rest[@]}" ); \
    ./gradlew "${tasks[@]}" --console=plain; \
  fi

build node:
  @if ! just list-nodes | grep -Fxq "{{node}}"; then \
    echo "Unknown node: {{node}}"; \
    exit 1; \
  fi
  @version="{{node}}"; loader="${version##*-}"; version="${version%-*}"; \
  ./gradlew --configure-on-demand ":$loader:$version:build" --console=plain

run-client node:
  @if ! just list-nodes | grep -Fxq "{{node}}"; then \
    echo "Unknown node: {{node}}"; \
    exit 1; \
  fi
  @version="{{node}}"; loader="${version##*-}"; version="${version%-*}"; \
  if [ "$version" = "1.16.5" ] && [ "$loader" = "forge" ]; then \
    ./gradlew --configure-on-demand ":forge:1.16.5:runLegacyClient" --console=plain; \
  else \
    ./gradlew --configure-on-demand ":$loader:$version:runClient" --console=plain; \
  fi

build-all:
  @./gradlew build --console=plain

boot-check node timeout="60":
  @if ! just list-nodes | grep -Fxq "{{node}}"; then \
    echo "Unknown node: {{node}}"; \
    exit 1; \
  fi
  @node="{{node}}"; \
  version="${node%-*}"; \
  loader="${node##*-}"; \
  effective_timeout="{{timeout}}"; \
  if [ "$version" = "1.18.1" ] && [ "$loader" = "forge" ] && [ "$effective_timeout" -lt 40 ]; then \
    effective_timeout=40; \
  fi; \
  gradle_task=":$loader:$version:runClient"; \
  if [ "$version" = "1.16.5" ] && [ "$loader" = "forge" ]; then \
    gradle_task=":forge:1.16.5:runLegacyClient"; \
  fi; \
  log="/tmp/konfig-$node.run.log"; \
  boot_marker='Konfig initialized'; \
  set +e; \
  ./gradlew --configure-on-demand "$gradle_task" --console=plain > "$log" 2>&1 & \
  gradle_pid=$!; \
  deadline=$(( $(date +%s) + effective_timeout )); \
  status=124; \
  while [ "$(date +%s)" -lt "$deadline" ]; do \
    if grep -q "$boot_marker" "$log"; then \
      status=124; \
      break; \
    fi; \
    if ! kill -0 "$gradle_pid" 2>/dev/null; then \
      wait "$gradle_pid"; \
      status=$?; \
      break; \
    fi; \
    sleep 1; \
  done; \
  if kill -0 "$gradle_pid" 2>/dev/null; then \
    kill "$gradle_pid" 2>/dev/null || true; \
    wait "$gradle_pid" || true; \
  fi; \
  pkill -f "$PWD/$loader/versions/$version/" 2>/dev/null || true; \
  set -e; \
  if [ "$status" -ne 0 ] && [ "$status" -ne 124 ]; then \
    tail -n 160 "$log"; \
    exit "$status"; \
  fi; \
  grep -q "$boot_marker" "$log"; \
  echo "Boot OK: $node (status=$status)"

boot-check-all timeout="60":
  @for node in $(just list-nodes); do \
    echo "==> $node"; \
    just boot-check "$node" "{{timeout}}"; \
  done

teakit-boot-check node timeout="60":
  @if ! just list-nodes | grep -Fxq "{{node}}"; then \
    echo "Unknown node: {{node}}"; \
    exit 1; \
  fi
  @node="{{node}}"; \
  version="${node%-*}"; \
  loader="${node##*-}"; \
  effective_timeout="{{timeout}}"; \
  if [ "$version" = "1.18.1" ] && [ "$loader" = "forge" ] && [ "$effective_timeout" -lt 40 ]; then \
    effective_timeout=40; \
  fi; \
  catalog="/home/kaf/code/mods/version-catalog/mc-$version/gradle/libs.versions.toml"; \
  if [ ! -f "$catalog" ] || ! rg -q '^teakit = ' "$catalog"; then \
    echo "TeaKit is not configured in the shared catalog for $version"; \
    exit 1; \
  fi; \
  gradle_task=":$loader:$version:runClient"; \
  if [ "$version" = "1.16.5" ] && [ "$loader" = "forge" ]; then \
    gradle_task=":forge:1.16.5:runLegacyClient"; \
  fi; \
  log="/tmp/konfig-$node.teakit.log"; \
  set +e; \
  ./gradlew --configure-on-demand "$gradle_task" --console=plain \
    -Dkonfig.withTeaKit=true \
    -Dteakit.autoExitTitle=true \
    -Dteakit.autoExitTitleDelayMs=2500 > "$log" 2>&1 & \
  gradle_pid=$!; \
  deadline=$(( $(date +%s) + effective_timeout )); \
  status=124; \
  while [ "$(date +%s)" -lt "$deadline" ]; do \
    if grep -q 'TeaKit scheduling clean shutdown from title screen' "$log"; then \
      status=124; \
      break; \
    fi; \
    if ! kill -0 "$gradle_pid" 2>/dev/null; then \
      wait "$gradle_pid"; \
      status=$?; \
      break; \
    fi; \
    sleep 1; \
  done; \
  if kill -0 "$gradle_pid" 2>/dev/null; then \
    kill "$gradle_pid" 2>/dev/null || true; \
    wait "$gradle_pid" || true; \
  fi; \
  pkill -f "$PWD/$loader/versions/$version/" 2>/dev/null || true; \
  set -e; \
  if [ "$status" -ne 0 ] && [ "$status" -ne 124 ]; then \
    tail -n 160 "$log"; \
    exit "$status"; \
  fi; \
  grep -q 'Konfig initialized' "$log"; \
  grep -q 'Initializing TeaKit on' "$log"; \
  grep -q 'TeaKit scheduling clean shutdown from title screen' "$log"; \
  echo "TeaKit boot OK: $node (status=$status)"

teakit-boot-check-all timeout="60":
  @for node in $(just list-nodes); do \
    version="${node%-*}"; \
    catalog="/home/kaf/code/mods/version-catalog/mc-$version/gradle/libs.versions.toml"; \
    if [ -f "$catalog" ] && rg -q '^teakit = ' "$catalog"; then \
      echo "==> $node"; \
      just teakit-boot-check "$node" "{{timeout}}"; \
    fi; \
  done

scenario-check node timeout="240":
  @./scripts/config-menu-scenario-node.sh "{{node}}" "{{timeout}}"

scenario-check-all timeout="240":
  @for node in $(just list-nodes); do \
    version="${node%-*}"; \
    catalog="/home/kaf/code/mods/version-catalog/mc-$version/gradle/libs.versions.toml"; \
    if [ -f "$catalog" ] && rg -q '^teakit = ' "$catalog"; then \
      echo "==> $node"; \
      just scenario-check "$node" "{{timeout}}"; \
    fi; \
  done
