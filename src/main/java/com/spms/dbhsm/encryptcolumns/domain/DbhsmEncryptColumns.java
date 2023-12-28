package com.spms.dbhsm.encryptcolumns.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.TreeEntity;

/**
 * 数据库加密列对象 dbhsm_encrypt_columns
 *
 * @author diq
 * @date 2023-09-27
 */
public class DbhsmEncryptColumns extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private String id;

    /** 数据库实例ID */
    private Long dbInstanceId;

    /** 数据库实例 */
    private String dbInstance;

    /** 用户名 */
    @Excel(name = "用户名")
    private String dbUserName;

    /** 数据库表 */
    @Excel(name = "数据库表")
    private String dbTable;

    /** 下发网口，web端所使用的网口，根据网口获取策略下载ip */
    private String ethernetPort;

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

    /** 列原始定义*/
    private String columnDefinitions;
    /** 密钥:软密钥*/
    private String secretKey;

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
    public void setDbInstanceId(Long dbInstanceId)
    {
        this.dbInstanceId = dbInstanceId;
    }

    public Long getDbInstanceId()
    {
        return dbInstanceId;
    }
    public void setDbInstance(String dbInstance)
    {
        this.dbInstance = dbInstance;
    }

    public String getDbInstance()
    {
        return dbInstance;
    }
    public void setDbUserName(String dbUserName)
    {
        this.dbUserName = dbUserName;
    }

    public String getDbUserName()
    {
        return dbUserName;
    }
    public void setDbTable(String dbTable)
    {
        this.dbTable = dbTable;
    }

    public String getDbTable()
    {
        return dbTable;
    }
    public void setEthernetPort(String ethernetPort)
    {
        this.ethernetPort = ethernetPort;
    }

    public String getEthernetPort()
    {
        return ethernetPort;
    }
    public void setEncryptColumns(String encryptColumns)
    {
        this.encryptColumns = encryptColumns;
    }

    public String getEncryptColumns()
    {
        return encryptColumns;
    }
    public void setColumnsType(String columnsType)
    {
        this.columnsType = columnsType;
    }

    public String getColumnsType()
    {
        return columnsType;
    }
    public void setEncryptionStatus(Integer encryptionStatus)
    {
        this.encryptionStatus = encryptionStatus;
    }

    public Integer getEncryptionStatus()
    {
        return encryptionStatus;
    }
    public void setEncryptionAlgorithm(String encryptionAlgorithm)
    {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public String getEncryptionAlgorithm()
    {
        return encryptionAlgorithm;
    }
    public void setEstablishRules(Integer establishRules)
    {
        this.establishRules = establishRules;
    }

    public Integer getEstablishRules()
    {
        return establishRules;
    }
    public void setEncryptionOffset(Integer encryptionOffset)
    {
        this.encryptionOffset = encryptionOffset;
    }

    public Integer getEncryptionOffset()
    {
        return encryptionOffset;
    }
    public void setEncryptionLength(Long encryptionLength)
    {
        this.encryptionLength = encryptionLength;
    }

    public Long getEncryptionLength()
    {
        return encryptionLength;
    }
    public void setSecretKeyId(String secretKeyId)
    {
        this.secretKeyId = secretKeyId;
    }

    public String getSecretKeyId()
    {
        return secretKeyId;
    }

    public String getColumnDefinitions() {
        return columnDefinitions;
    }

    public void setColumnDefinitions(String columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("dbInstanceId", getDbInstanceId())
            .append("dbInstance", getDbInstance())
            .append("dbUserName", getDbUserName())
            .append("dbTable", getDbTable())
            .append("ethernetPort", getEthernetPort())
            .append("encryptColumns", getEncryptColumns())
            .append("columnsType", getColumnsType())
            .append("encryptionStatus", getEncryptionStatus())
            .append("encryptionAlgorithm", getEncryptionAlgorithm())
            .append("establishRules", getEstablishRules())
            .append("encryptionOffset", getEncryptionOffset())
            .append("encryptionLength", getEncryptionLength())
            .append("secretKeyId", getSecretKeyId())
            .append("createTime", getCreateTime())
            .append("createBy", getCreateBy())
            .append("updateTime", getUpdateTime())
            .append("updateBy", getUpdateBy())
            .append("updateBy", getUpdateBy())
            .append("columnDefinitions", getColumnDefinitions())
            .append("secretKey", getSecretKey())
            .toString();
    }
}
