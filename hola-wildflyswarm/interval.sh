#!/bin/sh
for i in $(seq 1000); do
  curl http://localhost:9002/api/books2/1
  echo ""
  sleep 1
done;
