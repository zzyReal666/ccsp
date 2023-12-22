package com.spms.common;

import com.ccsp.common.core.utils.StringUtils;

/**
 * @project ccsp
 * @description ip
 * @author 18853
 * @date 2023/12/21 09:52:34
 * @version 1.0
 */
public class DBIpUtil {
    /**
     * 根据网口名称获取IP
     * @param ethernetPort
     * @return
     */
    public static String getIp(String ethernetPort) throws Exception {
        //获取端口
        String osName = System.getProperty("os.name");
        String ip = "";
        if (osName.toLowerCase().startsWith("linux")) {
            ip = CommandUtil.exeCmd("ip a| grep " + ethernetPort.split("@")[0] + " |grep inet |awk '{print $2}'|awk -F '/' '{print $1}'");
            if (StringUtils.isEmpty(ip)) {
                throw new Exception(ethernetPort + "口IP不存在,请先进行配置IP");
            }
            ip = ip.trim().replaceAll("\n", ",");
            if (StringUtils.isEmpty(ip)) {
                throw new Exception(ethernetPort + "口IP不存在,请先进行配置IP");
            }
            if (ip.lastIndexOf(",") == 0) {
                ip = ip.substring(0, ip.length() - 1);
            }

        }

        return ip.split(",")[0];
    }
}
