#!/bin/bash

DUMPDIR=/home/fnguyen/candlepin/perf-issue/localdumps
ADIR=/home/fnguyen/candlepin/perf-issue/adapters/20

buildr clean && server/bin/deploy -gm
cd $DUMPDIR
sleep 5
sudo systemctl stop tomcat
sudo rm -rf /var/log/candlepin/*
mysql -u candlepin candlepin < 20.sql
cd $ADIR
sleep 5
./install.sh
echo "Calling status resource"
sleep 10
curl -X GET http://localhost:8080/candlepin/status
