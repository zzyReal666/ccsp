package com.spms.dbhsm.encryptcolumns.domain.dto;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.TreeEntity;
import lombok.Data;
import lombok.ToString;

/**
 * 数据库加密列对象 dbhsm_encrypt_columns
 *
 * @author diq
 * @date 2023-09-27
 */
@Data
@ToString
public class DbhsmEncryptColumnsAdd extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private String id;

    /** 数据库实例ID */
    private Long dbInstanceId;

    /** 数据库实例 */
    private String dbInstance;

    /** 数据库类型 */
    private String databaseType;

    /** 数据库服务名 */
    private String databaseServerName;

    /** 用户名 */
    @Excel(name = "用户名")
    private String dbUserName;

    /** 数据库表 */
    @Excel(name = "数据库表")
    private String dbTable;

    /** 下发网口，web端所使用的网口，根据网口获取策略下载ip */
    private String ethernetPort;
    private String ipAndPort;

    /** 加密列 */
    @Excel(name = "加密列")
    private String encryptColumns;

    /** 列类型 */
    @Excel(name = "列类型")
    private String columnsType;

    /** 加密状态：1 未加密，2 已加密  3 已解密 */
    @Excel(name = "加密状态：1 未加密，2 已加密  3 已解密")
    private Integer encryptionStatus;

    /** 加密算法 */
    @Excel(name = "加密算法")
    private String encryptionAlgorithm;

    /** 是否建立规则0：否，1：是 */
    private Integer establishRules;

    /** 偏移量 */
    private Integer encryptionOffset;

    /** 加密长度 */
    private Long encryptionLength;

    /** 密钥ID：对应密钥表中的id主键字段 */
    private String secretKeyId;

    /** 密钥:软密钥*/
    private String secretKey;

    /** 列原始定义*/
    private String columnDefinitions;


}
