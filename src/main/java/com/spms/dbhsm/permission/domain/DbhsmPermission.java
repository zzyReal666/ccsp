package com.spms.dbhsm.permission.domain;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;

/**
 * 数据库权限对象 dbhsm_permission
 * 
 * @author diq
 * @date 2023-09-20
 */
@ApiModel("数据库权限对象")
public class DbhsmPermission extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 权限ID */
    private Long permissionId;

    /** 父权限ID */
    private Long parentId;

    /** 权限名称 */
    @Excel(name = "权限名称")
    @ApiModelProperty("权限名称")
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    /** 权限sql */
    @Excel(name = "权限sql")
    @ApiModelProperty("权限sql")
    @NotBlank(message = "权限sql不能为空")
    private String permissionSql;

    public void setPermissionId(Long permissionId)
    {
        this.permissionId = permissionId;
    }

    public Long getPermissionId()
    {
        return permissionId;
    }
    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Long getParentId()
    {
        return parentId;
    }
    public void setPermissionName(String permissionName)
    {
        this.permissionName = permissionName;
    }

    public String getPermissionName()
    {
        return permissionName;
    }
    public void setPermissionSql(String permissionSql)
    {
        this.permissionSql = permissionSql;
    }

    public String getPermissionSql()
    {
        return permissionSql;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("permissionId", getPermissionId())
            .append("parentId", getParentId())
            .append("permissionName", getPermissionName())
            .append("permissionSql", getPermissionSql())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
