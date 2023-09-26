package com.spms.dbhsm.dbInstance.domain.VO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据库实例对象 dbhsm_db_instance
 *
 * @date 2023-09-19
 */
@Data
public class InstanceServerNameVO
{

    /**
     * 实例ID
     */
    @ApiModelProperty(value = "实例ID")
    private int id;

    /**
     * 实例服务名
     */
    @ApiModelProperty(value = "实例服务名")
    private String label;

    /**
     * 分组是否被选中
     */
    @ApiModelProperty(value = "是否被选中")
    private boolean check;
}
