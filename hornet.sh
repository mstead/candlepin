#!/bin/bash
sudo cp candlepin.conf /etc/candlepin/candlepin.conf
sudo rm -rf /var/log/candlepin/*
cp test-config/persistence.xml server/src/main/resources/META-INF/
cp test-config/logback.xml server/src/main/resources/
buildr clean
#server/bin/deploy -gt
#to speed it up when DB is ready
server/bin/deploy 
#cd server; buildr rspec:consumer_resource_host_guest_spec:'guest should impose SLA on host auto-attach'
#cd server; buildr rspec:consumer_resource_spec
cd server; buildr rspec:consumer_resource_spec
