#!/bin/bash

curl -H "Content-type: application/json" -k -u admin:admin -X POST "https://localhost:8443/candlepin/consumers?owner=admin&username=filip" -d \
" {\"facts\":{}, \"name\":\"filipname\", \"type\":\"system\"}"

