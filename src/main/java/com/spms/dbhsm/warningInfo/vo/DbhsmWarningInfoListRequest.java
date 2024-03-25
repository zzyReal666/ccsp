package com.spms.dbhsm.warningInfo.vo;

import com.ccsp.common.core.annotation.Excel;
import com.ccsp.common.core.web.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-03-21 16:29 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class DbhsmWarningInfoListRequest extends BaseEntity {
    /** 主键 */
    private Long id;

    /** 配置ID */
    private Long configId;

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


    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(name = "startTime", value = "时间(范围-开始)")
    private String startTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(name = "endTime", value = "时间(范围-结束)")
    private String endTime;
}
