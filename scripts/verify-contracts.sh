#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
contract_dir=${1:-"$script_dir/../src/main/openapi"}
contract="$contract_dir/application-tracker.json"
source_file="$contract_dir/application-tracker.SOURCE"
checksums="$contract_dir/SHA256SUMS"
expected_checksum=549cebba300c2caf3403b9de01d3c34de84464a280a02183751e8a9583ad982a

for required_file in application-tracker.json application-tracker.SOURCE SHA256SUMS; do
    test -f "$contract_dir/$required_file" && test ! -L "$contract_dir/$required_file" || {
        echo "contract policy: $required_file is missing or symbolic" >&2
        exit 1
    }
done

test "$(wc -l < "$checksums" | tr -d ' ')" = 1
grep -Fx "$expected_checksum  application-tracker.json" "$checksums" >/dev/null
(cd "$contract_dir" && sha256sum --check --strict SHA256SUMS)

test "$(wc -l < "$source_file" | tr -d ' ')" = 4
grep -Fx 'repository=jobseekercopilot/application-tracker-service' "$source_file" >/dev/null
grep -Fx 'revision=d9e6bc9fcbe4ef665334c58672c8062b1e4796aa' "$source_file" >/dev/null
grep -Fx 'path=contracts/openapi.json' "$source_file" >/dev/null
grep -Fx "sha256=$expected_checksum" "$source_file" >/dev/null
test "$(sha256sum "$contract" | cut -d ' ' -f 1)" = "$expected_checksum"

jq -e '
    .openapi == "3.0.1"
    and .info.version == "1.1.0"
    and .paths["/api/v1/applications/user/{userId}"].get.operationId
        == "getApplicationsForUser"
    and ([.paths["/api/v1/applications/user/{userId}"].get.security[]
          | keys[]] | sort) == ["bearerAuth", "serviceToken"]
    and .paths["/api/v1/applications/user/{userId}"].get.responses["200"]
        .content["application/json"].schema.items["$ref"]
        == "#/components/schemas/ApplicationRecordResponse"
    and .paths["/api/v1/applications/user/{userId}"].get.responses["401"]
        .content["application/json"].schema["$ref"]
        == "#/components/schemas/SecurityErrorResponse"
    and .paths["/api/v1/applications/user/{userId}"].get.responses["403"]
        .content["application/json"].schema["$ref"]
        == "#/components/schemas/SecurityErrorResponse"
    and .paths["/api/v1/applications/user/{userId}"].get.responses["404"]
        .content["application/json"].schema["$ref"]
        == "#/components/schemas/ErrorResponse"
    and (["id", "userId", "jobId", "canonicalJobId", "provider",
          "externalJobId", "jobTitle", "companyName", "cvDocumentId",
          "coverLetterDocumentId", "status", "createdAt", "updatedAt"]
         - .components.schemas.ApplicationRecordResponse.required | length) == 0
' "$contract" >/dev/null

echo "Application Tracker contract input policy: passed"
