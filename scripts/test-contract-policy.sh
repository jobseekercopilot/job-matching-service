#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
source_dir="$script_dir/../src/main/openapi"
temporary_root=$(mktemp -d)
trap 'rm -rf "$temporary_root"' EXIT INT TERM

"$script_dir/verify-contracts.sh" "$source_dir" >/dev/null

copy_fixture() {
    fixture="$1"
    mkdir "$fixture"
    cp \
        "$source_dir/application-tracker.json" \
        "$source_dir/application-tracker.SOURCE" \
        "$source_dir/SHA256SUMS" \
        "$source_dir/location-service.yaml" \
        "$source_dir/location-service.pin.json" \
        "$fixture/"
}

assert_rejected() {
    fixture="$1"
    description="$2"
    if "$script_dir/verify-contracts.sh" "$fixture" >/dev/null 2>&1; then
        echo "contract policy test: $description was accepted" >&2
        exit 1
    fi
}

missing_contract="$temporary_root/missing-contract"
copy_fixture "$missing_contract"
rm "$missing_contract/application-tracker.json"
assert_rejected "$missing_contract" "missing contract"

missing_source="$temporary_root/missing-source"
copy_fixture "$missing_source"
rm "$missing_source/application-tracker.SOURCE"
assert_rejected "$missing_source" "missing provenance"

missing_location_contract="$temporary_root/missing-location-contract"
copy_fixture "$missing_location_contract"
rm "$missing_location_contract/location-service.yaml"
assert_rejected "$missing_location_contract" "missing location contract"

location_checksum_drift="$temporary_root/location-checksum-drift"
copy_fixture "$location_checksum_drift"
printf '%s\n' ' ' >> "$location_checksum_drift/location-service.yaml"
assert_rejected "$location_checksum_drift" "location contract checksum drift"

checksum_drift="$temporary_root/checksum-drift"
copy_fixture "$checksum_drift"
printf '%s\n' ' ' >> "$checksum_drift/application-tracker.json"
assert_rejected "$checksum_drift" "checksum drift"

provenance_drift="$temporary_root/provenance-drift"
copy_fixture "$provenance_drift"
sed -i 's/^revision=.*/revision=0000000000000000000000000000000000000000/' \
    "$provenance_drift/application-tracker.SOURCE"
assert_rejected "$provenance_drift" "provenance drift"

for mutation in operation response security required; do
    fixture="$temporary_root/missing-$mutation"
    copy_fixture "$fixture"
    case "$mutation" in
        operation)
            filter='del(.paths["/api/v1/applications/user/{userId}"].get.operationId)'
            ;;
        response)
            filter='del(.paths["/api/v1/applications/user/{userId}"].get.responses["401"])'
            ;;
        security)
            filter='.paths["/api/v1/applications/user/{userId}"].get.security = [{"bearerAuth":[]}]'
            ;;
        required)
            filter='.components.schemas.ApplicationRecordResponse.required -= ["canonicalJobId"]'
            ;;
    esac
    jq "$filter" "$fixture/application-tracker.json" \
        > "$fixture/application-tracker.changed"
    mv "$fixture/application-tracker.changed" "$fixture/application-tracker.json"
    (
        cd "$fixture"
        sha256sum application-tracker.json > SHA256SUMS
    )
    assert_rejected "$fixture" "missing required $mutation"
done

echo "Application Tracker contract policy tests: passed"
