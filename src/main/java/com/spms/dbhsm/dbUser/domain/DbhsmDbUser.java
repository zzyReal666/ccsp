package com.spms.dbhsm.dbUser.domain;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.permissionGroup.domain.dto.PermissionGroupForUserDto;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 数据库用户对象 dbhsm_db_users
 *
 * @author ccsp
 * @date 2023-09-25
 */
public class DbhsmDbUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户ID(待加密数据库中的ID) */
    private String userId;

    /** 权限组ID */
    @Excel(name = "权限组ID")
    private Long permissionGroupId;

    /** 数据库实例ID */
    @Excel(name = "数据库实例ID")
    private Long databaseInstanceId;

    /** 用户名 */
    @Excel(name = "用户名")
    private String userName;

    /** 密码 */
    @Excel(name = "密码")
    private String password;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date created;

    /** yes no */
    private String common;

    /** y */
    private String oracleMaintained;

    /** yes：系统用户，no:非系统用户 */
    private String inherited;

    /** USING_NLS_COMP */
    private String defaultCollation;

    /** yes no */
    private String implicit;

    /** yes no */
    private String allShard;

    /** 密码服务 */
    @Excel(name = "密码服务")
    private String secretService;

    /** 0:web端自建的用户 1：系统已有 */
    @Excel(name = "0:web端自建的用户 1：系统已有")
    private Integer isSelfBuilt;

    /** 0：系统用户 1：普通用户 */
    private Integer userRole;

    /** 表空间名称 */
    @Excel(name = "表空间名称")
    private String tableSpace;

    /** 加密插件地址 */
    @Excel(name = "加密插件地址")
    private String encLibapiPath;

    /** 数据库实例DTO */
    private DbInstanceGetConnDTO dbInstanceGetConnDTO;

    /** 权限组 */
    private PermissionGroupForUserDto permissionGroupForUserDto;

    public PermissionGroupForUserDto getPermissionGroupForUserDto() {
        return permissionGroupForUserDto;
    }

    public void setPermissionGroupForUserDto(PermissionGroupForUserDto permissionGroupForUserDto) {
        this.permissionGroupForUserDto = permissionGroupForUserDto;
    }

    public DbInstanceGetConnDTO getDbInstanceGetConnDTO() {
        return dbInstanceGetConnDTO;
    }

    public void setDbInstanceGetConnDTO(DbInstanceGetConnDTO dbInstanceGetConnDTO) {
        this.dbInstanceGetConnDTO = dbInstanceGetConnDTO;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getUserId()
    {
        return userId;
    }
    public void setPermissionGroupId(Long permissionGroupId)
    {
        this.permissionGroupId = permissionGroupId;
    }

    public Long getPermissionGroupId()
    {
        return permissionGroupId;
    }
    public void setDatabaseInstanceId(Long databaseInstanceId)
    {
        this.databaseInstanceId = databaseInstanceId;
    }

    public Long getDatabaseInstanceId()
    {
        return databaseInstanceId;
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
    public void setCreated(Date created)
    {
        this.created = created;
    }

    public Date getCreated()
    {
        return created;
    }
    public void setCommon(String common)
    {
        this.common = common;
    }

    public String getCommon()
    {
        return common;
    }
    public void setOracleMaintained(String oracleMaintained)
    {
        this.oracleMaintained = oracleMaintained;
    }

    public String getOracleMaintained()
    {
        return oracleMaintained;
    }
    public void setInherited(String inherited)
    {
        this.inherited = inherited;
    }

    public String getInherited()
    {
        return inherited;
    }
    public void setDefaultCollation(String defaultCollation)
    {
        this.defaultCollation = defaultCollation;
    }

    public String getDefaultCollation()
    {
        return defaultCollation;
    }
    public void setImplicit(String implicit)
    {
        this.implicit = implicit;
    }

    public String getImplicit()
    {
        return implicit;
    }
    public void setAllShard(String allShard)
    {
        this.allShard = allShard;
    }

    public String getAllShard()
    {
        return allShard;
    }
    public void setSecretService(String secretService)
    {
        this.secretService = secretService;
    }

    public String getSecretService()
    {
        return secretService;
    }
    public void setIsSelfBuilt(Integer isSelfBuilt)
    {
        this.isSelfBuilt = isSelfBuilt;
    }

    public Integer getIsSelfBuilt()
    {
        return isSelfBuilt;
    }
    public void setUserRole(Integer userRole)
    {
        this.userRole = userRole;
    }

    public Integer getUserRole()
    {
        return userRole;
    }
    public void setTableSpace(String tableSpace)
    {
        this.tableSpace = tableSpace;
    }

    public String getTableSpace()
    {
        return tableSpace;
    }
    public void setEncLibapiPath(String encLibapiPath)
    {
        this.encLibapiPath = encLibapiPath;
    }

    public String getEncLibapiPath()
    {
        return encLibapiPath;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("permissionGroupId", getPermissionGroupId())
            .append("databaseInstanceId", getDatabaseInstanceId())
            .append("userName", getUserName())
            .append("password", getPassword())
            .append("created", getCreated())
            .append("common", getCommon())
            .append("oracleMaintained", getOracleMaintained())
            .append("inherited", getInherited())
            .append("defaultCollation", getDefaultCollation())
            .append("implicit", getImplicit())
            .append("allShard", getAllShard())
            .append("secretService", getSecretService())
            .append("isSelfBuilt", getIsSelfBuilt())
            .append("userRole", getUserRole())
            .append("tableSpace", getTableSpace())
            .append("encLibapiPath", getEncLibapiPath())
            .toString();
    }
}
