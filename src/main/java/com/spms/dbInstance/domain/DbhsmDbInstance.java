package com.spms.dbInstance.domain;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 数据库实例对象 dbhsm_db_instance
 *
 * @author spms
 * @date 2023-09-19
 */
public class DbhsmDbInstance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 数据库类型 */
    @Excel(name = "数据库类型")
    private String databaseType;

    /** 数据库IP地址 */
    @Excel(name = "数据库IP地址")
    private String databaseIp;

    /** 数据库端口号 */
    @Excel(name = "数据库端口号")
    private String databasePort;

    /** 数据库服务名 */
    @Excel(name = "数据库服务名")
    private String databaseServerName;

    /** 实例类型 */
    @Excel(name = "实例类型 1 SID取值 : , 2 服务名取值 /")
    private String databaseExampleType;

    /** 数据库DBA */
    @Excel(name = "数据库DBA")
    private String databaseDba;

    /** 数据库DBA密码 */
    @Excel(name = "数据库DBA密码")
    private String databaseDbaPassword;

    /** 密码服务IP */
    @Excel(name = "密码服务IP")
    private String passwordServiceIp;

    /** 密码服务端口 */
    @Excel(name = "密码服务端口")
    private String passwordServicePort;

    /** 密码服务 */
    @Excel(name = "密码服务")
    private String secretService;

    /** 数据库版本号
    0:11g及以前版本,
    1:12.1.0.1.0-V38500 for linux x86,
    2:12.1.0.2.0-V77388 SE2 for linux x86(标准版),
    3:12.2.0.1.0-V839960 for linux x86,
    4:18.5.0.0.0,
    5:19.3 Enterprise Edition(包含标准版),
    6:19.5 Enterprise Edition(包含标准版),
    7:21.3 Enterprise Edition(包含标准版),
    8:12.1.0.1.0-V38894 for windows x86;
    9:12.1.0.2.0-V47115 for windows x86;
    10:12.1.0.2.0-V77408 SE2 for windows x86(标准版)
    11:12.1.0.2.0-V46095 release
    12:12.2.0.1.0-V839963 for windows x86 */
    @Excel(name = "数据库版本号 0:11g及以前版本,1:12.1.0.1.0-V38500 for linux x86,2:12.1.0.2.0-V77388 SE2 for linux x86(标准版),3:12.2.0.1.0-V839960 for linux x86,4:18.5.0.0.0,5:19.3 Enterprise Edition(包含标准版), 6:19.5 Enterprise Edition(包含标准版), 7:21.3 Enterprise Edition(包含标准版), 8:12.1.0.1.0-V38894 for windows x86; 9:12.1.0.2.0-V47115 for windows x86; 10:12.1.0.2.0-V77408 SE2 for windows x86(标准版) 11:12.1.0.2.0-V46095 release 12:12.2.0.1.0-V839963 for windows x86")
    private String databaseEdition;

    /** 用户创建模式：0：创建CDB容器中的公共用户 1：创建无容器数据库用户 */
    @Excel(name = "用户创建模式：0：创建CDB容器中的公共用户 1：创建无容器数据库用户")
    private Integer userCreateMode;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setDatabaseType(String databaseType)
    {
        this.databaseType = databaseType;
    }

    public String getDatabaseType()
    {
        return databaseType;
    }
    public void setDatabaseIp(String databaseIp)
    {
        this.databaseIp = databaseIp;
    }

    public String getDatabaseIp()
    {
        return databaseIp;
    }
    public void setDatabasePort(String databasePort)
    {
        this.databasePort = databasePort;
    }

    public String getDatabasePort()
    {
        return databasePort;
    }
    public void setDatabaseServerName(String databaseServerName)
    {
        this.databaseServerName = databaseServerName;
    }

    public String getDatabaseServerName()
    {
        return databaseServerName;
    }
    public void setDatabaseExampleType(String databaseExampleType)
    {
        this.databaseExampleType = databaseExampleType;
    }

    public String getDatabaseExampleType()
    {
        return databaseExampleType;
    }
    public void setDatabaseDba(String databaseDba)
    {
        this.databaseDba = databaseDba;
    }

    public String getDatabaseDba()
    {
        return databaseDba;
    }
    public void setDatabaseDbaPassword(String databaseDbaPassword)
    {
        this.databaseDbaPassword = databaseDbaPassword;
    }

    public String getDatabaseDbaPassword()
    {
        return databaseDbaPassword;
    }
    public void setPasswordServiceIp(String passwordServiceIp)
    {
        this.passwordServiceIp = passwordServiceIp;
    }

    public String getPasswordServiceIp()
    {
        return passwordServiceIp;
    }
    public void setPasswordServicePort(String passwordServicePort)
    {
        this.passwordServicePort = passwordServicePort;
    }

    public String getPasswordServicePort()
    {
        return passwordServicePort;
    }
    public void setSecretService(String secretService)
    {
        this.secretService = secretService;
    }

    public String getSecretService()
    {
        return secretService;
    }
    public void setDatabaseEdition(String databaseEdition)
    {
        this.databaseEdition = databaseEdition;
    }

    public String getDatabaseEdition()
    {
        return databaseEdition;
    }
    public void setUserCreateMode(Integer userCreateMode)
    {
        this.userCreateMode = userCreateMode;
    }

    public Integer getUserCreateMode()
    {
        return userCreateMode;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("databaseType", getDatabaseType())
            .append("databaseIp", getDatabaseIp())
            .append("databasePort", getDatabasePort())
            .append("databaseServerName", getDatabaseServerName())
            .append("databaseExampleType", getDatabaseExampleType())
            .append("databaseDba", getDatabaseDba())
            .append("databaseDbaPassword", getDatabaseDbaPassword())
            .append("passwordServiceIp", getPasswordServiceIp())
            .append("passwordServicePort", getPasswordServicePort())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("secretService", getSecretService())
            .append("databaseEdition", getDatabaseEdition())
            .append("userCreateMode", getUserCreateMode())
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DbhsmDbInstance)) {
            return false;
        }

        DbhsmDbInstance instance = (DbhsmDbInstance) o;

        if (getId() != null ? !getId().equals(instance.getId()) : instance.getId() != null) {
            return false;
        }
        if (getDatabaseType() != null ? !getDatabaseType().equals(instance.getDatabaseType()) : instance.getDatabaseType() != null) {
            return false;
        }
        if (getDatabaseIp() != null ? !getDatabaseIp().equals(instance.getDatabaseIp()) : instance.getDatabaseIp() != null) {
            return false;
        }
        if (getDatabasePort() != null ? !getDatabasePort().equals(instance.getDatabasePort()) : instance.getDatabasePort() != null) {
            return false;
        }
        if (getDatabaseServerName() != null ? !getDatabaseServerName().equals(instance.getDatabaseServerName()) : instance.getDatabaseServerName() != null) {
            return false;
        }
        if (getDatabaseExampleType() != null ? !getDatabaseExampleType().equals(instance.getDatabaseExampleType()) : instance.getDatabaseExampleType() != null) {
            return false;
        }
        if (getDatabaseDba() != null ? !getDatabaseDba().equals(instance.getDatabaseDba()) : instance.getDatabaseDba() != null) {
            return false;
        }
        if (getDatabaseDbaPassword() != null ? !getDatabaseDbaPassword().equals(instance.getDatabaseDbaPassword()) : instance.getDatabaseDbaPassword() != null) {
            return false;
        }
        if (getPasswordServiceIp() != null ? !getPasswordServiceIp().equals(instance.getPasswordServiceIp()) : instance.getPasswordServiceIp() != null) {
            return false;
        }
        if (getPasswordServicePort() != null ? !getPasswordServicePort().equals(instance.getPasswordServicePort()) : instance.getPasswordServicePort() != null) {
            return false;
        }
        if (getSecretService() != null ? !getSecretService().equals(instance.getSecretService()) : instance.getSecretService() != null) {
            return false;
        }
        if (getDatabaseEdition() != null ? !getDatabaseEdition().equals(instance.getDatabaseEdition()) : instance.getDatabaseEdition() != null) {
            return false;
        }
        return getUserCreateMode() != null ? getUserCreateMode().equals(instance.getUserCreateMode()) : instance.getUserCreateMode() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getDatabaseType() != null ? getDatabaseType().hashCode() : 0);
        result = 31 * result + (getDatabaseIp() != null ? getDatabaseIp().hashCode() : 0);
        result = 31 * result + (getDatabasePort() != null ? getDatabasePort().hashCode() : 0);
        result = 31 * result + (getDatabaseServerName() != null ? getDatabaseServerName().hashCode() : 0);
        result = 31 * result + (getDatabaseExampleType() != null ? getDatabaseExampleType().hashCode() : 0);
        result = 31 * result + (getDatabaseDba() != null ? getDatabaseDba().hashCode() : 0);
        result = 31 * result + (getDatabaseDbaPassword() != null ? getDatabaseDbaPassword().hashCode() : 0);
        result = 31 * result + (getPasswordServiceIp() != null ? getPasswordServiceIp().hashCode() : 0);
        result = 31 * result + (getPasswordServicePort() != null ? getPasswordServicePort().hashCode() : 0);
        result = 31 * result + (getSecretService() != null ? getSecretService().hashCode() : 0);
        result = 31 * result + (getDatabaseEdition() != null ? getDatabaseEdition().hashCode() : 0);
        result = 31 * result + (getUserCreateMode() != null ? getUserCreateMode().hashCode() : 0);
        return result;
    }
}
