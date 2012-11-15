#!/bin/bash
#
# Usage
#   sh ./runServer.sh
#

cd aa-server-10.1.0.1

TACAA_HOME=`pwd`
LIB=${TACAA_HOME}/lib
CLASSPATH=.
for i in $( ls ${LIB}/*.jar ); do
    CLASSPATH=${CLASSPATH}:$i
done


java -cp $CLASSPATH se.sics.tasim.sim.Main

cd ..