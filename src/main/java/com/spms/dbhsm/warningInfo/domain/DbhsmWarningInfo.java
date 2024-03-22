package com.spms.dbhsm.warningInfo.domain;

import lombok.Data;
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
@Data
public class DbhsmWarningInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

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

}
