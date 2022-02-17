#!/bin/sh

unset LANG

# LANG
##########################
LANG=ko_KR.utf8
export LANG

# SET LIBRARY
for i in ../lib/*.jar; do
    CP=$CP:$i
done
CP=`echo $CP | cut -c2-`


# JVM_ARGS for VM
##########################
JVM_ARGS="-Du=MARU_PAY -DCP_CONF=../conf -Dfile.encoding=utf-8 -Dlogback.configurationFile=../conf/logback.xml"
JVM_ARGS="$JVM_ARGS -Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2 -Djdk.tls.rejectClientInitiatedRenegotiation=true "
JVM_ARGS="$JVM_ARGS -Djdk.tls.ephemeralDHKeySize=2048 "
JVM_ARGS="$JVM_ARGS -Dorg.vertx.logger-delegate-factory-class-name=org.vertx.java.core.logging.impl.SLF4JLogDelegateFactory "
JVM_ARGS="$JVM_ARGS -Xss512k -Xms128m -Xmx256m "

java $JVM_ARGS com.pgmate.lib.vertx.main.VertXServer &
echo $!>apio.pid
