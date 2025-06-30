#!/bin/bash

set -e
set -o pipefail

ZAP_HOST="127.0.0.1"
ZAP_PORT="8090"
ZAP_URL="http://${ZAP_HOST}:${ZAP_PORT}"
ARTIFACT_DIR="<PATH_TO_OUTPUT_DIRECTORY>"                # e.g., /home/iudx/Downloads/zap-artifacts
REPORT_FILE="zap-report.html"
ZAP_BIN="<FULL_PATH_TO_DOWNLOADED_ZAP_SH>"               # e.g., /home/iudx/Downloads/ZAP_2.15.0/zap.sh  

# Parse arguments
MODE="$1"
COLLECTION="$2"  # Only required for --postman mode

if [[ "$MODE" != "--postman" && "$MODE" != "--mvn" ]]; then
  echo "Usage:"
  echo "  $0 --postman <collection-file>"
  echo "  $0 --mvn"
  exit 1
fi

# Start ZAP if not already running
echo "[*] Checking if ZAP is already running..."
if ! nc -z "$ZAP_HOST" "$ZAP_PORT"; then
  echo "[*] ZAP not running, starting in daemon mode..."
  nohup "$ZAP_BIN" -daemon -host "$ZAP_HOST" -port "$ZAP_PORT" -config api.disablekey=true > zap.log 2>&1 &
else
  echo "[*] ZAP is already running at $ZAP_HOST:$ZAP_PORT"
fi

# Wait for ZAP to become ready
echo "[*] Waiting for ZAP to become ready..."
until curl -s "${ZAP_URL}/JSON/core/view/version/" > /dev/null; do
  sleep 2
done
sleep 3

echo "[+] Creating new ZAP session..."
zap-cli --zap-url "http://$ZAP_HOST" --port "$ZAP_PORT" session new

# Execute API tests based on the mode
if [[ "$MODE" == "--postman" ]]; then
  if [[ ! -f "$COLLECTION" ]]; then
    echo "[!] Collection file not found: $COLLECTION"
    exit 1
  fi
  echo "[+] Running Postman collection through ZAP proxy..."
  HTTP_PROXY="http://${ZAP_HOST}:${ZAP_PORT}" \
  HTTPS_PROXY="http://${ZAP_HOST}:${ZAP_PORT}" \
  newman run "$COLLECTION" --insecure
elif [[ "$MODE" == "--mvn" ]]; then
  echo "[+] Running Rest Assured tests (Maven) through ZAP proxy..."
  MAVEN_OPTS="-Dhttp.proxyHost=$ZAP_HOST -Dhttp.proxyPort=$ZAP_PORT \
              -Dhttps.proxyHost=$ZAP_HOST -Dhttps.proxyPort=$ZAP_PORT" \
  mvn clean verify
fi

# Capture scanned URLs
echo "[+] Extracting accessed sites from ZAP..."
SITES=$(curl -s "${ZAP_URL}/JSON/core/view/sites/" | jq -r '.sites[]')

if [[ -z "$SITES" ]]; then
  echo "[!] No sites were captured by ZAP. Check your test execution or proxy config."
  exit 1
fi

# Perform spider and active scans
for URL in $SITES; do
  echo "[*] Running spider on $URL..."
  zap-cli --zap-url "http://$ZAP_HOST" --port "$ZAP_PORT" spider "$URL"

  echo "[*] Running active scan on $URL..."
  zap-cli --zap-url "http://$ZAP_HOST" --port "$ZAP_PORT" active-scan "$URL" --recursive
done

# Generate and move HTML report
echo "[+] Generating ZAP HTML report..."
zap-cli --zap-url "http://$ZAP_HOST" --port "$ZAP_PORT" report -o "$REPORT_FILE" -f html

echo "[+] Moving report to artifacts directory..."
mkdir -p "$ARTIFACT_DIR"
mv "$REPORT_FILE" "$ARTIFACT_DIR/"

echo "[✅] ZAP security testing completed successfully. Report available at $ARTIFACT_DIR/$REPORT_FILE"
