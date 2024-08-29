#!/bin/bash

# 定义错误码
ERR_MISSING_PARAMS=1
ERR_CREATE_DIR=2
ERR_COPY_FILES=3

# 函数: 输出错误信息并退出
error_exit() {
    local err_msg="$1"
    local err_code="$2"
    echo "ERROR: ${err_msg}" 1>&2
    exit "${err_code}"
}

# 检查参数数量
if [ "$#" -ne 1 ]; then
    error_exit "Usage: $0 <databaseId>" "${ERR_MISSING_PARAMS}"
fi

# 参数
DATABASE_ID="$1"

# 目录路径
BASE_DIR="/opt/dbenc/docker_v"
PROXY_DIR="${BASE_DIR}/proxy_${DATABASE_ID}"
CONF_DIR="${PROXY_DIR}/conf"
EXT_LIB_SRC="/opt/dbenc/ext-lib"

# 创建目录及子目录
echo "Creating directories..."
mkdir -p "${CONF_DIR}" || error_exit "Failed to create directory ${CONF_DIR}" "${ERR_CREATE_DIR}"

# 复制 ext-lib 目录
echo "Copying ext-lib directory..."
cp -r "${EXT_LIB_SRC}" "${PROXY_DIR}/" || error_exit "Failed to copy ${EXT_LIB_SRC}​⬤