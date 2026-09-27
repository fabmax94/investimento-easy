#!/usr/bin/env bash
# Prepara um Android SDK mínimo para compilar e testar o projeto (CI e ambientes remotos).
#
# Não precisa do sdkmanager: o Android Gradle Plugin baixa sozinho a plataforma e o
# build-tools que faltarem, desde que as licenças estejam aceitas. Requer acesso a
# dl.google.com e maven.google.com.
set -euo pipefail

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
mkdir -p "$SDK_DIR/licenses"

# Hashes públicos das licenças do Android SDK (mesmo conteúdo que `sdkmanager --licenses` grava).
printf '\n24333f8a63b6825ea9c5514f83c2829b004d1fee\nd56f5187479451eabf01fb78af6dfcb131a6481e\n8933bad161af4178b1185d1a37fbf41ea5269c55' \
  > "$SDK_DIR/licenses/android-sdk-license"
printf '\n84831b9409646a918e30573bab4c9c91346d8abd' > "$SDK_DIR/licenses/android-sdk-preview-license"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ ! -f "$ROOT/local.properties" ] || ! grep -q '^sdk.dir=' "$ROOT/local.properties"; then
  echo "sdk.dir=$SDK_DIR" >> "$ROOT/local.properties"
fi
echo "Android SDK em $SDK_DIR"
