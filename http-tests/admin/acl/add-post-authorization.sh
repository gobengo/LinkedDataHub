#!/bin/bash
set -euo pipefail

initialize_dataset "$END_USER_BASE_URL" "$TMP_END_USER_DATASET" "$END_USER_ENDPOINT_URL"
initialize_dataset "$ADMIN_BASE_URL" "$TMP_ADMIN_DATASET" "$ADMIN_ENDPOINT_URL"
purge_backend_cache "$END_USER_VARNISH_SERVICE"
purge_backend_cache "$ADMIN_VARNISH_SERVICE"

# access is unauthorized

curl -k -w "%{http_code}\n" -o /dev/null -s \
  -E "${AGENT_CERT_FILE}":"${AGENT_CERT_PWD}" \
  -H "Content-Type: application/n-triples" \
  -H "Accept: application/n-triples" \
  -X POST \
  "${END_USER_BASE_URL}" \
| grep -q "${STATUS_FORBIDDEN}"

pushd . > /dev/null && cd "$SCRIPT_ROOT/admin/acl"

# create authorization

./create-authorization.sh \
  -f "$OWNER_CERT_FILE" \
  -p "$OWNER_CERT_PWD" \
  -b "$ADMIN_BASE_URL" \
  --label "POST authorization" \
  --agent "$AGENT_URI" \
  --to "$END_USER_BASE_URL" \
  --append

popd > /dev/null

# access is allowed after authorization is created

curl -k -w "%{http_code}\n" -o /dev/null -f -s \
  -E "${AGENT_CERT_FILE}":"${AGENT_CERT_PWD}" \
  -H "Content-Type: application/n-triples" \
  -H "Accept: application/n-triples" \
  -H "Content-Length: 0" \
  -X POST \
  "${END_USER_BASE_URL}" \
| grep -q "${STATUS_NO_CONTENT}"