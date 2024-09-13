#!/bin/bash

export JAVA_HOME=/opt/spms/jdk1.8.0_341
export CLASSPATH=.:${JAVA_HOME}/lib/rt.jar:${JAVA_HOME}/dt.jar:${JAVA_HOME}/tools.jar
export PATH=$PATH:${JAVA_HOME}/bin

spms_path=/opt/spms/spms
export SPMS_DBENC=$spms_path/spms-dbenc-manager/spms-dbenc-manager.jar
export SPMS_DBENC_CONFIG=$spms_path/spms-dbenc-manager/bootstrap.yml

case "$1" in

start)

  echo "--------等待 $SPMS_DBENC 启动--------------"
  P_COUNT=$(ps -ef | grep -v 'grep' | grep -c $SPMS_DBENC)
  if [ $P_COUNT -eq 0 ]; then
    nohup java -jar -Xmx256m -Dspring.config.location=$SPMS_DBENC_CONFIG $SPMS_DBENC > $SPMS_DBENC.log 2>&1 &
    P_ID=$(ps -ef | grep $SPMS_DBENC | grep -v 'grep' | awk '{print $2}' | head -1)
    until [ -n "$P_ID" ]; do
      sleep 3
      P_ID=$(ps -ef | grep $SPMS_DBENC | grep -v 'grep' | awk '{print $2}' | head -1)
    done
    echo "$SPMS_DBENC pid is $P_ID"
    echo "---------$SPMS_DBENC 启动成功-----------"
  else
    echo "$SPMS_DBENC 已经在运行，不需要再次启动"
  fi
  ;;

stop)

  P_ID=$(ps -ef | grep $SPMS_DBENC | grep -v "grep" | awk '{print $2}' | head -1)
  if [ -n "$P_ID" ]; then
    ps -ef | grep $SPMS_DBENC | grep -v "grep" | awk '{print $2}' | xargs kill -9
    echo "$SPMS_DBENC 已停止"
  else
    echo "$SPMS_DBENC 进程不存在或已停止"
  fi
  ;;

restart)
  $0 stop
  echo "===重启 $SPMS_DBENC==="
  $0 start
  echo "===重启成功==="
  ;;

*)
  echo "Usage: {start|stop|restart}"
  ;;
esac

exit 0