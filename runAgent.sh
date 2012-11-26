#!/bin/bash
#
# Usage
#   sh ./runAgent.sh -file [agent filename]
#

TACAA_HOME=`dirname $0`
LIB=${TACAA_HOME}/lib
CLASSPATH=.
for i in $( ls ${LIB}/*.jar ); do
    CLASSPATH=${CLASSPATH}:$i
done

echo $1

javac -cp $CLASSPATH AgentOrange.java
java -server -cp $CLASSPATH edu.umich.eecs.tac.aa.agentware.Main -config config/o1.conf
