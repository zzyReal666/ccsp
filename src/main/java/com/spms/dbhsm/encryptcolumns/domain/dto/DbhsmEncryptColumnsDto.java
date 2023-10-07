package com.spms.dbhsm.encryptcolumns.domain.dto;

import com.ccsp.common.core.annotation.Excel;
import lombok.Data;
import lombok.ToString;

/**
 * 数据库加密列查询DTO dbhsm_encrypt_columns
 *
 * @author diq
 * @date 2023-09-27
 */
@Data
@ToString
public class DbhsmEncryptColumnsDto
{
    private static final long serialVersionUID = 1L;

    /** 数据库实例ID */
    private Long dbInstanceId;


    /** 用户名ID */
    private Long dbUserId;

    /** 用户名 */
    @Excel(name = "用户名")
    private String dbUserName;

    /** 数据库表 */
    @Excel(name = "数据库表")
    private String dbTableName;

    /** 下发网口，web端所使用的网口，根据网口获取策略下载ip */
    private String ethernetPort;


}
