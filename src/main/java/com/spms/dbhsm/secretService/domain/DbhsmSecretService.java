package com.spms.dbhsm.secretService.domain;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 密码服务对象 dbhsm_secret_service
 * 
 * @author diq
 * @date 2023-09-25
 */
public class DbhsmSecretService extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 密码服务 */
    @Excel(name = "密码服务")
    @NotBlank(message = "密码服务不能为空")
    private String secretService;

    /** ip */
    @Excel(name = "ip")
    @NotBlank(message = "ip不能为空")
    private String serviceIp;

    /** 端口 */
    @Excel(name = "端口")
    @NotBlank(message = "端口不能为空")
    @Pattern(regexp = "^[1-9]+[0-9]*$", message = "端口不合法")
    private String servicePort;

    /** URL */
    @Excel(name = "URL")
    private String serviceUrl;

    /** 状态：1启动、0停止 */
    @Excel(name = "状态：1启动、0停止")
    private Integer status;

    /** 用户名 */
    @Excel(name = "用户名")
    private String userName;

    /** 密码 */
    private String password;

    /** 密钥索引：用于生成SM2密钥，其公钥用于配置到KMIP */
    @Excel(name = "密钥索引：用于生成SM2密钥，其公钥用于配置到KMIP")
    private Long secretKeyIndex;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setSecretService(String secretService) 
    {
        this.secretService = secretService;
    }

    public String getSecretService() 
    {
        return secretService;
    }
    public void setServiceIp(String serviceIp) 
    {
        this.serviceIp = serviceIp;
    }

    public String getServiceIp() 
    {
        return serviceIp;
    }
    public void setServicePort(String servicePort) 
    {
        this.servicePort = servicePort;
    }

    public String getServicePort() 
    {
        return servicePort;
    }
    public void setServiceUrl(String serviceUrl) 
    {
        this.serviceUrl = serviceUrl;
    }

    public String getServiceUrl() 
    {
        return serviceUrl;
    }
    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }
    public void setUserName(String userName) 
    {
        this.userName = userName;
    }

    public String getUserName() 
    {
        return userName;
    }
    public void setPassword(String password) 
    {
        this.password = password;
    }

    public String getPassword() 
    {
        return password;
    }
    public void setSecretKeyIndex(Long secretKeyIndex) 
    {
        this.secretKeyIndex = secretKeyIndex;
    }

    public Long getSecretKeyIndex() 
    {
        return secretKeyIndex;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("secretService", getSecretService())
            .append("serviceIp", getServiceIp())
            .append("servicePort", getServicePort())
            .append("serviceUrl", getServiceUrl())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("createBy", getCreateBy())
            .append("updateTime", getUpdateTime())
            .append("updateBy", getUpdateBy())
            .append("userName", getUserName())
            .append("password", getPassword())
            .append("secretKeyIndex", getSecretKeyIndex())
            .toString();
    }
}
