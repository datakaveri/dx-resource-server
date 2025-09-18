#!/bin/bash

set -e

ZAP_HOST="10.139.0.10"
ZAP_PORT="8090"
ARTIFACT_DIR="/var/lib/jenkins/iudx/rs/zap-artifacts"
REPORT_FILE="zap-report.html"
TARGET_API="https://rs.iudx.io/apis"

# Parse args
MODE="$1"
COLLECTION="$2"
ENV_FILE="$3"

# Show usage if invalid
if [[ "$MODE" != "--postman" && "$MODE" != "--mvn" ]]; then
  echo "Usage:"
  echo "  $0 --postman <collection-file> <env-file>"
  echo "  $0 --mvn"
  exit 1
fi

echo "[+] Creating new ZAP session..."
zap-cli --zap-url "http://${ZAP_HOST}" --port "$ZAP_PORT" session new

# For Postman mode
if [[ "$MODE" == "--postman" ]]; then
  if [[ ! -f "$COLLECTION" ]]; then
    echo "[!] Collection file not found: $COLLECTION"
    exit 1
  fi
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "[!] Environment file not found: $ENV_FILE"
    exit 1
  fi

  echo "[+] Running Postman collection through ZAP proxy..."
  HTTP_PROXY="http://${ZAP_HOST}:${ZAP_PORT}" \
  newman run "$COLLECTION" \
    -e "$ENV_FILE" \
    -n 2 \
    --insecure \
    -r htmlextra \
    --reporter-htmlextra-export "${ARTIFACT_DIR}/${REPORT_FILE}" \
    --reporter-htmlextra-skipSensitiveData
fi

# For Maven mode
if [[ "$MODE" == "--mvn" ]]; then
  echo "[+] Running Maven integration tests through ZAP proxy..."
  sudo update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java
  mvn test-compile failsafe:integration-test \
    -DskipUnitTests=true \
    -DintTestProxyHost=jenkins-master-priv \
    -DintTestProxyPort=8090 \
    -DintTestHost=jenkins-slave1 \
    -DintTestPort=8080
fi

# Spider and scan ONLY the resource server API
echo "[+] Running ZAP spider and active scan on ${TARGET_API}..."
zap-cli --zap-url "http://${ZAP_HOST}" --port "$ZAP_PORT" spider "$TARGET_API" --max-depth 2 --context-name Default

zap-cli --zap-url "http://${ZAP_HOST}" --port "$ZAP_PORT" active-scan "$TARGET_API" --recursive --policy "Light"

# Generate report (if mvn)
if [[ "$MODE" == "--mvn" ]]; then
  echo "[+] Generating ZAP HTML report..."
  zap-cli --zap-url "http://${ZAP_HOST}" --port "$ZAP_PORT" report -o "$REPORT_FILE" -f html
  mkdir -p "$ARTIFACT_DIR"
  mv "$REPORT_FILE" "$ARTIFACT_DIR/"
fi

echo "[✅] ZAP scan/report completed. Report path: $ARTIFACT_DIR/$REPORT_FILE"
