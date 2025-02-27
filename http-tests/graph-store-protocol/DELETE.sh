#!/bin/bash
set -euo pipefail

initialize_dataset "$END_USER_BASE_URL" "$TMP_END_USER_DATASET" "$END_USER_ENDPOINT_URL"
initialize_dataset "$ADMIN_BASE_URL" "$TMP_ADMIN_DATASET" "$ADMIN_ENDPOINT_URL"
purge_backend_cache "$END_USER_VARNISH_SERVICE"
purge_backend_cache "$ADMIN_VARNISH_SERVICE"

pushd . > /dev/null && cd "$SCRIPT_ROOT"

# create container

slug="test"

container=$(./create-container.sh \
  -f "$OWNER_CERT_FILE" \
  -p "$OWNER_CERT_PWD" \
  -b "$END_USER_BASE_URL" \
  --title "Test" \
  --slug "$slug" \
  --parent "$END_USER_BASE_URL")

popd > /dev/null

pushd . > /dev/null && cd "$SCRIPT_ROOT/admin/acl"

# add an explicit read/write authorization for the owner because add-agent-to-group.sh won't work non-existing URI

./create-authorization.sh \
-b "$ADMIN_BASE_URL" \
  -f "$OWNER_CERT_FILE" \
  -p "$OWNER_CERT_PWD" \
  --label "Write base" \
  --agent "$AGENT_URI" \
  --to "$container" \
  --read \
  --write

popd > /dev/null

# delete document

curl -k -w "%{http_code}\n" -o /dev/null -f -s -G \
  -E "$AGENT_CERT_FILE":"$AGENT_CERT_PWD" \
  -X DELETE \
  "$container" \
| grep -q "$STATUS_NO_CONTENT"

# check that the graph is gone

curl -k -w "%{http_code}\n" -o /dev/null -s -G \
  -E "$AGENT_CERT_FILE":"$AGENT_CERT_PWD" \
  -H "Accept: application/n-triples" \
  "$container" \
| grep -q "$STATUS_NOT_FOUND"