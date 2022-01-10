chcp 65001

d:
cd D:\Develop\workspace\MARU_PAY\bin

java -DCP_CONF=../conf -Dlogback.configurationFile=../conf/logback.xml -Dfile.encoding=UTF-8 -Dvertx.options.blockedThreadCheckInterval=60000 -Dorg.vertx.logger-delegate-factory-class-name=org.vertx.java.core.logging.impl.SLF4JLogDelegateFactory -cp ../lib/*;../lib/spring/*;../classes../war com.pgmate.lib.vertx.main.VertXServer
pause