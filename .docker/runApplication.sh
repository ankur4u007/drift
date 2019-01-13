#!/bin/bash

serverOn=${TUNNEL__SERVER__ENABLED}
serverPort=${TUNNEL__SERVER__PORT}
env=""

if [ "$serverOn" == "true" ] || [ "$serverOn" == "TRUE" ] ; then
    env=$env" -DSPRING__MAIN__WEB-APPLICATION-TYPE=REACTIVE"
    if [ -v "$serverPort" ]; then
        env=$env" -DSERVER__PORT=${serverPort}"
    fi
else
   env=$env" -DSPRING__MAIN__WEB-APPLICATION-TYPE=NONE"
fi

cmdToRun="java $env -jar drift.jar"
echo "starting command: $cmdToRun"
$cmdToRun