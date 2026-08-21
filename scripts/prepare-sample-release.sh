#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"
cd "${project_dir}"

if [[ ! -f keystore.properties ]]; then
    echo "keystore.properties is required for a public sample release." >&2
    echo "Copy keystore.properties.example and reference an organization-managed key outside this repository." >&2
    exit 1
fi

./gradlew testDebugUnitTest lintDebug assembleRelease

version_name="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)"/\1/p' app/build.gradle.kts | head -n 1)"
if [[ -z "${version_name}" ]]; then
    echo "Could not determine versionName from app/build.gradle.kts." >&2
    exit 1
fi

source_apk="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "${source_apk}" ]]; then
    echo "Signed release APK was not generated: ${source_apk}" >&2
    exit 1
fi

apksigner_command="$(command -v apksigner || true)"
if [[ -z "${apksigner_command}" && -n "${ANDROID_HOME:-}" ]]; then
    apksigner_command="$(find "${ANDROID_HOME}/build-tools" -type f -name apksigner 2>/dev/null | sort | tail -n 1)"
fi
if [[ -z "${apksigner_command}" && -n "${ANDROID_SDK_ROOT:-}" ]]; then
    apksigner_command="$(find "${ANDROID_SDK_ROOT}/build-tools" -type f -name apksigner 2>/dev/null | sort | tail -n 1)"
fi
if [[ -z "${apksigner_command}" ]]; then
    echo "apksigner is required to verify the public sample APK." >&2
    exit 1
fi

"${apksigner_command}" verify --verbose --print-certs "${source_apk}"

mkdir -p dist
artifact_name="qlm29h-rtk-${version_name}-sample-release.apk"
artifact="dist/${artifact_name}"
cp "${source_apk}" "${artifact}"

if command -v shasum >/dev/null 2>&1; then
    (cd dist && shasum -a 256 "${artifact_name}" > "${artifact_name}.sha256")
elif command -v sha256sum >/dev/null 2>&1; then
    (cd dist && sha256sum "${artifact_name}" > "${artifact_name}.sha256")
else
    echo "A SHA-256 command (shasum or sha256sum) is required." >&2
    exit 1
fi

echo "Public sample artifact: ${artifact}"
echo "Checksum: ${artifact}.sha256"
