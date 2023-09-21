package com.spms.dbhsm.permissionGroup.domain;

import com.ccsp.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 权限和权限组关联对象 dbhsm_permission_union_permission_group
 * 
 * @author diq
 * @date 2023-09-20
 */
public class DbhsmPermissionUnionPermissionGroup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 权限ID */
    private Long permissionId;

    /** 权限组ID */
    private Long permissionGroupId;

    public void setPermissionId(Long permissionId) 
    {
        this.permissionId = permissionId;
    }

    public Long getPermissionId() 
    {
        return permissionId;
    }
    public void setPermissionGroupId(Long permissionGroupId) 
    {
        this.permissionGroupId = permissionGroupId;
    }

    public Long getPermissionGroupId() 
    {
        return permissionGroupId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("permissionId", getPermissionId())
            .append("permissionGroupId", getPermissionGroupId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
