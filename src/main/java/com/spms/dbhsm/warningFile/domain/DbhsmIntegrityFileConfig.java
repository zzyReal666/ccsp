package com.spms.dbhsm.warningFile.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;

/**
 * 文件完整性校验对象 dbhsm_integrity_file_config
 * 
 * @author diq
 * @date 2024-04-17
 */
public class DbhsmIntegrityFileConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 文件路径 */
    @Excel(name = "文件路径")
    private String filePath;

    /** 校验方式 */
    @Excel(name = "校验方式")
    private Long verificationType;

    /** 校验值 */
    @Excel(name = "校验值")
    private String verificationValue;

    /** 轮询时间(分钟) */
    @Excel(name = "轮询时间(分钟)")
    private String cron;

    /** 任务状态 */
    @Excel(name = "任务状态")
    private Integer enableTiming;

    /** 任务名称 */
    private String jobName;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setFilePath(String filePath) 
    {
        this.filePath = filePath;
    }

    public String getFilePath() 
    {
        return filePath;
    }
    public void setVerificationType(Long verificationType) 
    {
        this.verificationType = verificationType;
    }

    public Long getVerificationType() 
    {
        return verificationType;
    }
    public void setVerificationValue(String verificationValue) 
    {
        this.verificationValue = verificationValue;
    }

    public String getVerificationValue() 
    {
        return verificationValue;
    }
    public void setCron(String cron) 
    {
        this.cron = cron;
    }

    public String getCron() 
    {
        return cron;
    }
    public void setEnableTiming(Integer enableTiming) 
    {
        this.enableTiming = enableTiming;
    }

    public Integer getEnableTiming() 
    {
        return enableTiming;
    }
    public void setJobName(String jobName) 
    {
        this.jobName = jobName;
    }

    public String getJobName() 
    {
        return jobName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("filePath", getFilePath())
            .append("verificationType", getVerificationType())
            .append("verificationValue", getVerificationValue())
            .append("cron", getCron())
            .append("enableTiming", getEnableTiming())
            .append("jobName", getJobName())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
