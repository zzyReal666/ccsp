package com.spms.dbhsm.secretKey.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 数据库密钥对象 dbhsm_secret_key_manage
 *
 * @author diq
 * @date 2023-09-22
 */
public class DbhsmSecretKeyManage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    private Long id;

    /** 密钥id */
    @Excel(name = "密钥id")
    private String secretKeyId;

    /** 密钥索引 */
    @Excel(name = "密钥索引")
    @NotNull(message = "密钥索引不能为空")
    private Long secretKeyIndex;

    /** 密钥名称 */
    @Excel(name = "密钥名称")
    @NotBlank(message = "密钥名称不能为空")
    private String secretKeyName;

    /** 密钥类型 */
    @Excel(name = "密钥类型")
    @NotNull(message = "请选择密钥类型")
    private Integer secretKeyType;

    /** 密钥状态 */
    private Integer secretKeyStatus;

    /** 密钥算法 */
    @Excel(name = "密钥算法")
    private Integer secretKeyAlgorithm;

    /** 密钥来源 */
    @Excel(name = "密钥来源")
    @NotNull(message = "请选择密钥来源")
    private Integer secretKeySource;

    /** 密钥服务名称 */
    private String secretKeyServer;

    /** 密钥数据 */
    private String secretKey;

    /** 密钥长度 */
    @Excel(name = "密钥长度")
    @NotNull(message = "请选择密钥长度")
    private Integer secretKeyLength;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setSecretKeyId(String secretKeyId)
    {
        this.secretKeyId = secretKeyId;
    }

    public String getSecretKeyId()
    {
        return secretKeyId;
    }
    public void setSecretKeyIndex(Long secretKeyIndex)
    {
        this.secretKeyIndex = secretKeyIndex;
    }

    public Long getSecretKeyIndex()
    {
        return secretKeyIndex;
    }
    public void setSecretKeyName(String secretKeyName)
    {
        this.secretKeyName = secretKeyName;
    }

    public String getSecretKeyName()
    {
        return secretKeyName;
    }
    public void setSecretKeyType(Integer secretKeyType)
    {
        this.secretKeyType = secretKeyType;
    }

    public Integer getSecretKeyType()
    {
        return secretKeyType;
    }
    public void setSecretKeyStatus(Integer secretKeyStatus)
    {
        this.secretKeyStatus = secretKeyStatus;
    }

    public Integer getSecretKeyStatus()
    {
        return secretKeyStatus;
    }
    public void setSecretKeyAlgorithm(Integer secretKeyAlgorithm)
    {
        this.secretKeyAlgorithm = secretKeyAlgorithm;
    }

    public Integer getSecretKeyAlgorithm()
    {
        return secretKeyAlgorithm;
    }
    public void setSecretKeySource(Integer secretKeySource)
    {
        this.secretKeySource = secretKeySource;
    }

    public Integer getSecretKeySource()
    {
        return secretKeySource;
    }
    public void setSecretKeyServer(String secretKeyServer)
    {
        this.secretKeyServer = secretKeyServer;
    }

    public String getSecretKeyServer()
    {
        return secretKeyServer;
    }
    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getSecretKey()
    {
        return secretKey;
    }
    public void setSecretKeyLength(Integer secretKeyLength)
    {
        this.secretKeyLength = secretKeyLength;
    }

    public Integer getSecretKeyLength()
    {
        return secretKeyLength;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("secretKeyId", getSecretKeyId())
                .append("secretKeyIndex", getSecretKeyIndex())
                .append("secretKeyName", getSecretKeyName())
                .append("secretKeyType", getSecretKeyType())
                .append("secretKeyStatus", getSecretKeyStatus())
                .append("secretKeyAlgorithm", getSecretKeyAlgorithm())
                .append("secretKeySource", getSecretKeySource())
                .append("secretKeyServer", getSecretKeyServer())
                .append("secretKey", getSecretKey())
                .append("secretKeyLength", getSecretKeyLength())
                .append("createTime", getCreateTime())
                .append("createBy", getCreateBy())
                .append("updateTime", getUpdateTime())
                .append("updateBy", getUpdateBy())
                .toString();
    }
}
