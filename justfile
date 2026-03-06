set shell := ["bash", "-cu"]

default:
  @just --list

# Resolved at runtime so new version folders are picked up automatically.
version := shell("ls -1d */ | sed 's:/$::' | grep -E '^[0-9]' | sort -V | tail -n1")

list-versions:
  @ls -1d */ | sed 's:/$::' | grep -E '^[0-9]' | sort -V

versions: list-versions

latest:
  @echo {{version}}

loader-enabled version loader:
  @if [ ! -f "{{version}}/settings.gradle" ]; then echo "false"; exit 0; fi
  @if (command -v rg >/dev/null 2>&1 && rg -q "^[[:space:]]*include\\([\"']{{loader}}[\"']\\)|^[[:space:]]*include[[:space:]]+[\"']{{loader}}[\"']" "{{version}}/settings.gradle") || \
     (! command -v rg >/dev/null 2>&1 && grep -Eq "^[[:space:]]*include\\([\"']{{loader}}[\"']\\)|^[[:space:]]*include[[:space:]]+[\"']{{loader}}[\"']" "{{version}}/settings.gradle"); then \
    echo "true"; \
  else \
    echo "false"; \
  fi

gradle version *args:
  @if [ ! -d "{{version}}" ]; then echo "Version {{version}} not found."; exit 1; fi
  @cd "{{version}}" && chmod +x gradlew && ./gradlew {{args}}

run version *args:
  @just gradle "{{version}}" {{args}}

build version="":
  @if [ -z "{{version}}" ]; then \
    for v in $(just list-versions); do \
      echo "==> $v"; \
      for loader in fabric forge neoforge; do \
        if [ "$(just loader-enabled "$v" "$loader")" = "true" ]; then \
          just build-loader "$v" "$loader"; \
        else \
          echo "Skipping $v:$loader (not included in settings.gradle)"; \
        fi; \
      done; \
    done; \
  else \
    for loader in fabric forge neoforge; do \
      if [ "$(just loader-enabled "{{version}}" "$loader")" = "true" ]; then \
        just build-loader "{{version}}" "$loader"; \
      else \
        echo "Skipping {{version}}:$loader (not included in settings.gradle)"; \
      fi; \
    done; \
  fi

build-loader version loader *args:
  @if [ "$(just loader-enabled "{{version}}" "{{loader}}")" = "true" ]; then \
    just gradle "{{version}}" :{{loader}}:build {{args}}; \
  else \
    echo "Skipping {{version}}:{{loader}} (not included in settings.gradle)"; \
  fi

test version="":
  @if [ -z "{{version}}" ]; then \
    for v in $(just list-versions); do \
      echo "==> $v"; \
      just gradle "$v" test; \
    done; \
  else \
    just gradle "{{version}}" test; \
  fi

publish version="":
  @if [ -z "{{version}}" ]; then \
    for v in $(just list-versions); do \
      echo "==> $v"; \
      just publish-version "$v"; \
    done; \
  else \
    just publish-version "{{version}}"; \
  fi

publish-version version:
  @just publish-common "{{version}}"
  @for loader in fabric forge neoforge; do \
    if [ "$(just loader-enabled "{{version}}" "$loader")" = "true" ]; then \
      just publish-loader "{{version}}" "$loader"; \
    else \
      echo "Skipping {{version}}:$loader (not included in settings.gradle)"; \
    fi; \
  done

publish-common version *args:
  @just gradle "{{version}}" :common:publishAllPublicationsToKafMavenRepository {{args}}

publish-loader version loader *args:
  @if [ "$(just loader-enabled "{{version}}" "{{loader}}")" = "true" ]; then \
    just gradle "{{version}}" :{{loader}}:publishAllPublicationsToKafMavenRepository {{args}}; \
  else \
    echo "Skipping {{version}}:{{loader}} (not included in settings.gradle)"; \
  fi
