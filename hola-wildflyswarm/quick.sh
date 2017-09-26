#!/bin/sh
sleep 5
for i in $(seq 20); do
  curl http://localhost:9002/api/books3/1
  echo ""
done;
