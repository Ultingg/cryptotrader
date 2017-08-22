#!/bin/bash

pushd "`dirname "$0"`" > /dev/null || exit $?

java -cp "`find . -name "winstone-*.jar"`" winstone.tools.WinstoneControl shutdown --host=127.0.01 --port=40422

RET=$?

sleep 3

ps -ef | grep java | grep winstone

popd > /dev/null 2>&1

exit $RET