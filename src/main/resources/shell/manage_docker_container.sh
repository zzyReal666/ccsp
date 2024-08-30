#!/bin/bash

# 定义错误码
ERR_MISSING_PARAMS=1
ERR_INVALID_ACTION=4
ERR_DOCKER_COMMAND=5
ERR_FIREWALLD=6

# 函数: 输出错误信息并退出
error_exit() {
    local err_msg="$1"
    local err_code="$2"
    echo "ERROR: ${err_msg}" 1>&2
    exit "${err_code}"
}

# 检查参数数量
if [ "$#" -lt 2 ]; then
    error_exit "Usage: $0 <start|stop|restart|remove|status> <database_id> [<port>]" "${ERR_MISSING_PARAMS}"
fi

# 参数
ACTION="$1"          # 操作类型: start, stop, restart, remove, status
DATABASE_ID="$2"    # 数据库实例ID, 用于生成容器名字和目录
PORT="$3"           # 端口号，仅在start操作时需要

# Docker 镜像名称
IMAGE_NAME="proxy:5.5.0"
BASE_DIR="/opt/db_enc/docker_v"
PROXY_DIR="${BASE_DIR}/proxy_${DATABASE_ID}"
CONF_DIR="${PROXY_DIR}/conf"
EXT_LIB_DIR="${PROXY_DIR}/ext-lib"
CONTAINER_NAME="proxy_${DATABASE_ID}"

# 执行操作
case "${ACTION}" in
    start)
        if [ -z "${PORT}" ]; then
            error_exit "Missing port for start action" "${ERR_MISSING_PARAMS}"
        fi
        echo "Starting Docker container ${CONTAINER_NAME}..."
        docker run -d \
            --name "${CONTAINER_NAME}" \
            -v "${CONF_DIR}:/opt/shardingsphere-proxy/conf" \
            -v "${EXT_LIB_DIR}:/opt/shardingsphere-proxy/ext-lib" \
            -e PORT="3308" \
            -p "${PORT}:3308" \
            --restart always \
            "${IMAGE_NAME}" || error_exit "Failed to start Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        echo "Configuring firewall rules..."
        firewall-cmd --zone=public --add-port="${PORT}/tcp" --permanent || error_exit "Failed to add port ${PORT} to firewalld" "${ERR_FIREWALLD}"
        firewall-cmd --reload || error_exit "Failed to reload firewalld" "${ERR_FIREWALLD}"
        echo "Container ${CONTAINER_NAME} started and configured successfully."
        ;;

    stop)
        echo "Stopping Docker container ${CONTAINER_NAME}..."
        docker stop "${CONTAINER_NAME}" || error_exit "Failed to stop Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        echo "Container ${CONTAINER_NAME} stopped successfully."
        ;;

    restart)
        echo "Restarting Docker container ${CONTAINER_NAME}..."
        docker restart "${CONTAINER_NAME}" || error_exit "Failed to restart Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        echo "Container ${CONTAINER_NAME} restarted successfully."
        ;;

    remove)
        echo "Removing Docker container ${CONTAINER_NAME}..."
        docker rm -f "${CONTAINER_NAME}" || error_exit "Failed to remove Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        echo "Container ${CONTAINER_NAME} removed successfully."
        ;;

    status)
        echo "Checking status of Docker container ${CONTAINER_NAME}..."
        docker ps -a --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\t{{.Status}}" || error_exit "Failed to get status of Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        ;;

    *)
        error_exit "Invalid action: ${ACTION}. Valid actions are start, stop, restart, remove, status." "${ERR_INVALID_ACTION}"
        ;;
esac

# 成功退出
exit 0