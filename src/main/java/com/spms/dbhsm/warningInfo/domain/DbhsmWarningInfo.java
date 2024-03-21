package com.spms.dbhsm.warningInfo.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;

/**
 * warningInfo对象 dbhsm_warning_info
 * 
 * @author diq
 * @date 2024-03-21
 */
public class DbhsmWarningInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 校验状态 */
    private Long status;

    /** 校验结果 */
    @Excel(name = "校验结果")
    private String result;

    /** 原校验值 */
    @Excel(name = "原校验值")
    private String oldVerificationValue;

    /** 校验值 */
    @Excel(name = "校验值")
    private String newVerificationValue;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setStatus(Long status) 
    {
        this.status = status;
    }

    public Long getStatus() 
    {
        return status;
    }
    public void setResult(String result) 
    {
        this.result = result;
    }

    public String getResult() 
    {
        return result;
    }
    public void setOldVerificationValue(String oldVerificationValue) 
    {
        this.oldVerificationValue = oldVerificationValue;
    }

    public String getOldVerificationValue() 
    {
        return oldVerificationValue;
    }
    public void setNewVerificationValue(String newVerificationValue) 
    {
        this.newVerificationValue = newVerificationValue;
    }

    public String getNewVerificationValue() 
    {
        return newVerificationValue;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("status", getStatus())
            .append("result", getResult())
            .append("oldVerificationValue", getOldVerificationValue())
            .append("newVerificationValue", getNewVerificationValue())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
