#!/bin/bash
# Corre las pruebas, compila el APK firmado y lo deja como Filtro.apk
set -o pipefail
chmod +x gradlew
./gradlew --no-daemon testReleaseUnitTest assembleRelease 2>&1 | tee build.log
status=${PIPESTATUS[0]}
if [ "$status" -eq 0 ]; then
  cp app/build/outputs/apk/release/app-release.apk Filtro.apk
fi
exit "$status"
