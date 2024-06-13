package com.spms.dbhsm.stockDataProcess.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * @author zzypersonally@gmail.com
 * @description 用于存量数据加密的DTO对象
 * @since 2024/4/28 15:13
 */
@Data
public class DatabaseDTO {

    /**
     * 数据库实例ID
     */
    private Long id;

    /**
     * 数据库的存储模式
     */
    private DbStorageMode dbStorageMode;

    /**
     * 数据库类型 0：Oracle 1：SQLServer 2:mysql 3:PostgreSQL 4：DM8,
     */
    private String databaseType;

    /**
     * 数据库版本
     */
    private String databaseVersion;

    /**
     * 数据库IP
     */
    private String databaseIp;

    /**
     * 数据库端口
     */
    private String databasePort;

    /**
     * dba账号
     */
    private String databaseDba;

    /**
     * dba密码
     */
    private String databaseDbaPassword;

    /**
     * 业务账号
     */
    private String serviceUser;

    /**
     * 业务密码
     */
    private String servicePassword;

    /**
     * 实例类型 1 SID, 2 服务名（Oracle使用）
     */
    private String instanceType;

    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * 连接URL
     */
    private String connectUrl;


    /**
     * 待加密的表
     */
    List<TableDTO> tableDTOList;


    /**
     * 数据库存储模式
     */
    public enum DbStorageMode {
        ROW, COLUMN
    }


}
