package com.spms.dbhsm.permissionGroup.domain.dto;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;
import com.spms.dbhsm.permission.domain.DbhsmPermission;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * 数据库权限组信息对象 dbhsm_permission_group
 * 
 * @author diq
 * @date 2023-09-20
 */
public class PermissionGroupEditDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 权限组ID */
    private Long permissionGroupId;

    /** 权限组名称 */
    @Excel(name = "权限组名称")
    private String permissionGroupName;

    /** 角色状态（0正常 1停用） */
    @Excel(name = "角色状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 权限和权限组关联信息 */
    private List<DbhsmPermission> dbhsmPermissionUnionPermissionGroupList;

    public void setPermissionGroupId(Long permissionGroupId) 
    {
        this.permissionGroupId = permissionGroupId;
    }

    public Long getPermissionGroupId() 
    {
        return permissionGroupId;
    }
    public void setPermissionGroupName(String permissionGroupName) 
    {
        this.permissionGroupName = permissionGroupName;
    }

    public String getPermissionGroupName() 
    {
        return permissionGroupName;
    }
    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public List<DbhsmPermission> getDbhsmPermissionUnionPermissionGroupList() {
        return dbhsmPermissionUnionPermissionGroupList;
    }

    public void setDbhsmPermissionUnionPermissionGroupList(List<DbhsmPermission> dbhsmPermissionUnionPermissionGroupList) {
        this.dbhsmPermissionUnionPermissionGroupList = dbhsmPermissionUnionPermissionGroupList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("permissionGroupId", getPermissionGroupId())
            .append("permissionGroupName", getPermissionGroupName())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("dbhsmPermissionUnionPermissionGroupList", getDbhsmPermissionUnionPermissionGroupList())
            .toString();
    }
}
