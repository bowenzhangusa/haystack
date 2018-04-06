#!/bin/bash -e

PORT=''

while [[ $# -gt 0 ]]; do
  PORT="$1"
  shift
done

if [[ -z "$PORT" ]] || [[ $# -ne 0 ]]; then
  echo "  Usage: there must be a port number supplied to the server" >&2
  echo "" >&2
  echo >&2
  exit 1
fi

export HAYSTACK_HOME="`pwd`"
echo "[INFO] Running: com.haystack.server.HaystackServer on port number $PORT)"
echo "start listening:...."
exec mvn -q -nsu compile exec:exec -Dcheckstyle.skip=true  -DmainClass="com.haystack.server.HaystackServer" -Dport="$PORT"
