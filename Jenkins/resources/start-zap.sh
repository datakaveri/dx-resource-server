#!/bin/bash

set -e

ZAP_BIN="/home/ubuntu/ZAP_2.16.1/zap.sh"
ZAP_HOST="0.0.0.0"
ZAP_PORT="8090"
ZAP_URL="http://${ZAP_HOST}:${ZAP_PORT}"

echo "[*] Checking if ZAP is already running..."
if ! nc -z "$ZAP_HOST" "$ZAP_PORT"; then
  echo "[*] Starting ZAP in daemon mode..."
  nohup "$ZAP_BIN" -daemon \
    -host "$ZAP_HOST" \
    -port "$ZAP_PORT" \
    -config api.disablekey=true \
    -config api.addrs.addr.name=.* \
    -config api.addrs.addr.regex=true > zap.log 2>&1 &
else
  echo "[*] ZAP already running at $ZAP_URL"
fi

echo "[*] Waiting for ZAP to become ready..."
until curl -s "${ZAP_URL}/JSON/core/view/version/" > /dev/null; do
  sleep 2
done

echo "[✅] ZAP is ready and listening at $ZAP_URL"
