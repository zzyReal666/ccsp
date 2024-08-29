#!/bin/bash

# 定义错误码
ERR_MISSING_PARAMS=1
ERR_INVALID_ACTION=2
ERR_DOCKER_COMMAND=3
ERR_FIREWALLD=4

# 日志文件
LOG_FILE="/var/log/manage_docker_container.log"

# 函数: 输出日志信息
log_message() {
    echo "$(date +'%Y-%m-%d %H:%M:%S') - $1" >> "${LOG_FILE}"
}

# 函数: 输出错误信息并退出
error_exit() {
    local err_msg="$1"
    local err_code="$2"
    log_message "ERROR: ${err_msg}"
    echo "${err_msg}" 1>&2
    exit "${err_code}"
}

# 检查参数数量
if [ "$#" -lt 2 ]; then
    error_exit "Usage: $0 <start|stop|restart|remove|status> <container_name> [<param1> <port>]" "${ERR_MISSING_PARAMS}"
fi

# 参数
ACTION="$1"
CONTAINER_NAME="$2"
PARAM1="$3"
PORT="$4"

# Docker 镜像名称
IMAGE_NAME="apache/shardingsphere-proxy:5.4.1"
BASE_DIR="/opt/dbenc/docker_v"
PROXY_DIR="${BASE_DIR}/proxy_${PARAM1}"
CONF_DIR="${PROXY_DIR}/conf"
EXT_LIB_DIR="${PROXY_DIR}/ext-lib"

# 执行操作
case "${ACTION}" in
    start)
        if [ -z "${PARAM1}" ] || [ -z "${PORT}" ]; then
            error_exit "Missing param1 or port for start action" "${ERR_MISSING_PARAMS}"
        fi
        log_message "Starting Docker container ${CONTAINER_NAME}..."
        docker run -d \
            --name "${CONTAINER_NAME}" \
            -v "${CONF_DIR}:/opt/shardingsphere-proxy/conf" \
            -v "${EXT_LIB_DIR}:/opt/shardingsphere-proxy/ext-lib" \
            -e PORT="${PORT}" \
            -p "${PORT}:3308" \
            "${IMAGE_NAME}" || error_exit "Failed to start Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        log_message "Configuring Docker container to restart unless stopped..."
        docker update --restart unless-stopped "${CONTAINER_NAME}" || error_exit "Failed to set container ${CONTAINER_NAME} to restart unless stopped" "${ERR_DOCKER_COMMAND}"
        log_message "Configuring firewall rules..."
        firewall-cmd --zone=public --add-port="${PORT}/tcp" --permanent || error_exit "Failed to add port ${PORT} to firewalld" "${ERR_FIREWALLD}"
        firewall-cmd --reload || error_exit "Failed to reload firewalld" "${ERR_FIREWALLD}"
        log_message "Container ${CONTAINER_NAME} started and configured successfully."
        ;;

    stop)
        log_message "Stopping Docker container ${CONTAINER_NAME}..."
        docker stop "${CONTAINER_NAME}" || error_exit "Failed to stop Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        log_message "Container ${CONTAINER_NAME} stopped successfully."
        ;;

    restart)
        log_message "Restarting Docker container ${CONTAINER_NAME}..."
        docker restart "${CONTAINER_NAME}" || error_exit "Failed to restart Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        log_message "Container ${CONTAINER_NAME} restarted successfully."
        ;;

    remove)
        log_message "Removing Docker container ${CONTAINER_NAME}..."
        docker rm -f "${CONTAINER_NAME}" || error_exit "Failed to remove Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        log_message "Container ${CONTAINER_NAME} removed successfully."
        ;;

    status)
        log_message "Checking status of Docker container ${CONTAINER_NAME}..."
        docker ps -a --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\t{{.Status}}" || error_exit "Failed to get status of Docker container ${CONTAINER_NAME}" "${ERR_DOCKER_COMMAND}"
        ;;

    *)
        error_exit "Invalid action: ${ACTION}. Valid actions are start, stop, restart, remove, status." "${ERR_INVALID_ACTION}"
        ;;
esac

# 成功退出
exit 0