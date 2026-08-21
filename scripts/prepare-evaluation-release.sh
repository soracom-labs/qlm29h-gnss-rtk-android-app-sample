#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"
cd "${project_dir}"

./gradlew testDebugUnitTest assembleDebug lintDebug

version_name="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)"/\1/p' app/build.gradle.kts | head -n 1)"
if [[ -z "${version_name}" ]]; then
    echo "Could not determine versionName from app/build.gradle.kts." >&2
    exit 1
fi

mkdir -p dist
artifact_name="qlm29h-rtk-${version_name}-evaluation-debug.apk"
artifact="dist/${artifact_name}"
cp app/build/outputs/apk/debug/app-debug.apk "${artifact}"

if command -v shasum >/dev/null 2>&1; then
    (cd dist && shasum -a 256 "${artifact_name}" > "${artifact_name}.sha256")
elif command -v sha256sum >/dev/null 2>&1; then
    (cd dist && sha256sum "${artifact_name}" > "${artifact_name}.sha256")
else
    echo "A SHA-256 command (shasum or sha256sum) is required." >&2
    exit 1
fi

echo "Evaluation artifact: ${artifact}"
echo "Checksum: ${artifact}.sha256"
