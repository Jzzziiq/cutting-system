#!/bin/sh
set -e
cd "$(dirname "$0")/.."
docker-compose up -d --build
echo "Done. Visit http://localhost"
