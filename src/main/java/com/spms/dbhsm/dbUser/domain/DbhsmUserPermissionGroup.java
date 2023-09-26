package com.spms.dbhsm.dbUser.domain;

import com.ccsp.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户与权限组关联关系对象 dbhsm_user_permission_group
 *
 * @author Kong
 * @date 2023-09-26
 */
public class DbhsmUserPermissionGroup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户id，对应用户表中的id字段 */
    private Long userId;

    /** 权限组id */
    private Long permissionGroupId;

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("permissionGroupId", getPermissionGroupId())
            .toString();
    }
}
