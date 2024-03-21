package com.spms.dbhsm.warningConfig.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;

/**
 * warningConfig对象 dbhsm_warning_config
 * 
 * @author diq
 * @date 2024-03-21
 */

@Data
public class DbhsmWarningConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 需要校验的字段信息 */
    @Excel(name = "需要校验的字段信息")
    private String tableFields;

    /** 数据库连接信息 */
    @Excel(name = "数据库连接信息")
    private String databaseConnectionInfo;

    /** 表信息 */
    @Excel(name = "表信息")
    private String databaseTableInfo;

    /** 校验方式 */
    @Excel(name = "校验方式")
    private Long verificationType;

    private String verificationFields;

    /** 定时任务表达式 */
    @Excel(name = "定时任务表达式")
    private String cron;

    /** 是否启用定时任务 */
    @Excel(name = "是否启用定时任务")
    private Integer enableTiming;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String jobName;

}
