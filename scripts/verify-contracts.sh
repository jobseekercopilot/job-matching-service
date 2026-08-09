#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
contract_dir=${1:-"$script_dir/../src/main/openapi"}
contract="$contract_dir/application-tracker.json"
source_file="$contract_dir/application-tracker.SOURCE"
checksums="$contract_dir/SHA256SUMS"
location_contract="$contract_dir/location-service.yaml"
location_pin="$contract_dir/location-service.pin.json"
expected_checksum=7722467906230b37c48aee583254115c16ea663e284bb4e6e020abb292693d78
expected_location_checksum=22238272fb50d69c49abb3fda167e41897749c6fb67651c8e3b045d13817a2f5

for required_file in application-tracker.json application-tracker.SOURCE SHA256SUMS \
    location-service.yaml location-service.pin.json; do
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
grep -Fx 'revision=09ec630e69c61d5be650fce55722c0b85b335bd8' "$source_file" >/dev/null
grep -Fx 'path=contracts/openapi.json' "$source_file" >/dev/null
grep -Fx "sha256=$expected_checksum" "$source_file" >/dev/null
test "$(sha256sum "$contract" | cut -d ' ' -f 1)" = "$expected_checksum"
test "$(sha256sum "$location_contract" | cut -d ' ' -f 1)" = "$expected_location_checksum"
jq -e --arg checksum "$expected_location_checksum" '
    .schemaVersion == 1
    and .repository == "jobseekercopilot/location-service"
    and .contractPath == "api/openapi.yaml"
    and .contractVersion == "1.0.0"
    and .sourceRevision == "1ccaa0c153262ca4dda3c9bac9866bee30c6f3e7"
    and .sha256 == $checksum
    and .generatedClient.generator == "openapi-generator"
    and .generatedClient.generatorVersion == "7.5.0"
' "$location_pin" >/dev/null
grep -Fx '  /internal/v1/commutes:matrix:' "$location_contract" >/dev/null

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
