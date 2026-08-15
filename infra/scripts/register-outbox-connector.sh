#!/usr/bin/env bash
set -euo pipefail

CONNECT_URL="http://localhost:8083"
CONNECTOR_NAME="notification-outbox-connector"
CONFIG_FILE="$(dirname "$0")/../kafka-connect/outbox-connector.json"

echo "Waiting for Kafka Connect to be ready..."
until curl -sf "${CONNECT_URL}/connectors" > /dev/null; do
  sleep 2
done

if curl -sf "${CONNECT_URL}/connectors/${CONNECTOR_NAME}" > /dev/null 2>&1; then
  echo "Connector already registered. Updating config..."
  curl -X PUT "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/config" \
    -H "Content-Type: application/json" \
    -d @<(jq '.config' "${CONFIG_FILE}")
else
  echo "Registering connector..."
  curl -X POST "${CONNECT_URL}/connectors" \
    -H "Content-Type: application/json" \
    -d @"${CONFIG_FILE}"
fi

echo ""
echo "Connector status:"
curl -s "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status" | jq

# keeping it in the repo as a manual re-run tool for whenever you edit outbox-connector.json
# mid-session without wanting to tear down the whole stack — but it's no longer required for a clean start.