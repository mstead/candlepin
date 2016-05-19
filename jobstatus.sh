#!/bin/bash


curl -X GET -k -u admin:admin "https://localhost:8443/candlepin/jobs/bind_by_pool_92876bcc-7e27-429e-9755-597580c60483" | jq '.' 

