#!/usr/bin/env bash
set -euo pipefail

target="app/google-services.json"

if [[ -f "$target" ]]; then
  exit 0
fi

mkdir -p "$(dirname "$target")"

cat > "$target" <<'JSON'
{
  "project_info": {
    "project_number": "0",
    "project_id": "knittools-ci-placeholder",
    "storage_bucket": "knittools-ci-placeholder.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:0:android:0",
        "android_client_info": {
          "package_name": "com.finnvek.knittools"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "ci-placeholder-api-key"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
JSON
