package com.spms.dbhsm.warningConfig.vo;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-03-21 16:29 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class DbhsmWarningConfigListRequest extends BaseEntity {
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


    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(name = "startTime", value = "时间(范围-开始)")
    private Date startTime;

    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(name = "endTime", value = "时间(范围-结束)")
    private Date endTime;
}
