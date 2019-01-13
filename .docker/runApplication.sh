#!/bin/sh

serverOn=${TUNNEL__SERVER__ENABLED}
env=""

if [ "$serverOn" == "true" ] || [ "$serverOn" == "TRUE" ] ; then
    env=$env" -DSPRING__MAIN__WEB-APPLICATION-TYPE=REACTIVE"
else
   env=$env" -Dspring.main.web-application-type=NONE"
fi

cmdToRun="java$env -jar drift.jar"
echo "starting command: $cmdToRun"
$cmdToRun