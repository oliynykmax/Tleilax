#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

find "$repo_root/book/src/assets" -type f \( -name '*.png' -o -name '*.svg' \) -delete
find "$repo_root/book/src/assets" -type f -name '*.puml' -print0 | xargs -0 plantuml -tpng
