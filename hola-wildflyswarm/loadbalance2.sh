#!/bin/sh
for i in $(seq 10); do
  curl http://localhost:9000/api/books4/1
  echo ""
  sleep 1
done;
