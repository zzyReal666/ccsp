package com.spms.dbhsm.warningInfo.vo;

import com.ccsp.common.core.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-03-22 10:53 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class DbhsmWarningInfoListResponse {

    /** 主键 */
    private Long id;

    /** 主键 */
    private Long configId;

    /** 校验状态 */
    private Long status;

    /** 校验结果 */
    @Excel(name = "告警信息")
    private String result;

    /** 原校验值 */
    @Excel(name = "原校验值")
    private String oldVerificationValue;

    /** 校验值 */
    @Excel(name = "计算校验值")
    private String newVerificationValue;

    @Excel(name = "告警时间")
    @ApiModelProperty(value = "创建时间" , hidden = true)
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @ApiModelProperty(value = "连接信息")
    private String connectionInfo;

    /** 数据库类型 */
    private String databaseType;

    /** 数据库IP地址 */
    private String databaseIp;

    /** 数据库端口号 */
    private String databasePort;

    /** 数据库服务名 */
    private String databaseServerName;

    /** 实例类型 */
    private String databaseExampleType;
}
