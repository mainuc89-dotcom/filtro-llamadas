#!/bin/bash
# Sube logs y capturas a la rama "ci-resultados" para revisarlos sin entrar a GitHub.
# Uso: push-results.sh <nombre> <estado> <archivo-o-carpeta>...
set -u
NAME="$1"; STATUS="$2"; shift 2
BR=ci-resultados
RUN="${GITHUB_RUN_NUMBER:-0}"
WORK=$(mktemp -d)
URL="https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
git config --global user.name "github-actions[bot]"
git config --global user.email "41898282+github-actions[bot]@users.noreply.github.com"
if ! git clone -q --depth 1 --branch "$BR" "$URL" "$WORK" 2>/dev/null; then
  git -C "$WORK" init -q
  git -C "$WORK" checkout -q -b "$BR"
  git -C "$WORK" remote add origin "$URL"
fi
DEST="$WORK/run-$RUN/$NAME"
mkdir -p "$DEST"
for src in "$@"; do
  [ -e "$src" ] && cp -r "$src" "$DEST/"
done
echo "$STATUS $(git rev-parse --short HEAD 2>/dev/null)" > "$DEST/ESTADO"
git -C "$WORK" add -A
git -C "$WORK" commit -q -m "Resultados $NAME #$RUN ($STATUS)"
for i in 1 2 3 4; do
  git -C "$WORK" push -q origin "$BR" && exit 0
  git -C "$WORK" pull -q --rebase origin "$BR" || true
  sleep 3
done
echo "No se pudieron subir los resultados"
