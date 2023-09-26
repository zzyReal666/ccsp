package com.spms.dbhsm.dbUser.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;

/**
 * 用户与数据库实例关联对象 dbhsm_user_db_instance
 *
 * @author Kong
 * @date 2023-09-26
 */
public class DbhsmUserDbInstance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户id */
    private Long userId;

    /** 实例id */
    private Long instanceId;

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }
    public void setInstanceId(Long instanceId)
    {
        this.instanceId = instanceId;
    }

    public Long getInstanceId()
    {
        return instanceId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("instanceId", getInstanceId())
            .toString();
    }
}
