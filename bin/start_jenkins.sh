#!/bin/sh
unset LANG

export JAVA_PATH=/usr/local/jdk1.8.0_271/bin
export MARU_PATH=/home/bkwinners/MARU_PAY

# LANG
##########################
LANG=ko_KR.utf8
export LANG

# SET LIBRARY
for i in $MARU_PATH/lib/*.jar; do
    CP=$CP:$i
done
CP=`echo $CP | cut -c2-`

# JVM_ARGS for VM
##########################
JVM_ARGS="-DMARU_PAY -server -DCP_CONF=$MARU_PATH/conf -Dlogback.configurationFile=$MARU_PATH/conf/logback.xml -Dfile.encoding=UTF-8"
JVM_ARGS="$JVM_ARGS -Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2 -Djdk.tls.rejectClientInitiatedRenegotiation=true "
JVM_ARGS="$JVM_ARGS -Djdk.tls.ephemeralDHKeySize=2048 "
JVM_ARGS="$JVM_ARGS -Dorg.vertx.logger-delegate-factory-class-name=org.vertx.java.core.logging.impl.SLF4JLogDelegateFactory "
JVM_ARGS="$JVM_ARGS -Xss512k -Xms128m -Xmx512m"
JVM_ARGS="$JVM_ARGS -cp $CP:$MARU_PATH/war"

nohup $JAVA_PATH/java $JVM_ARGS com.pgmate.pay.main.VertXServerNew > /dev/null 2>&1 &
echo $!>apio.pid
