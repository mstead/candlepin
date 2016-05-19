#!/bin/bash

#first unbind all
curl -H "Content-type: application/json" -k -u admin:admin -X DELETE "https://localhost:8443/candlepin/consumers/ccacb525-962a-41cb-8bd5-7a0d8b8ad5cd/entitlements" 

sleep 5
echo "BINDING..."
curl -H "Content-type: application/json" -k -u admin:admin -X POST "https://localhost:8443/candlepin/consumers/ccacb525-962a-41cb-8bd5-7a0d8b8ad5cd/entitlements?async=true&pool=8aa8846154c97d340154c97dec8008ea" 

