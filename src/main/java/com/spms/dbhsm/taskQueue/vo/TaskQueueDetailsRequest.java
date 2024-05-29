package com.spms.dbhsm.taskQueue.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-22 10:22 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class TaskQueueDetailsRequest {


    @ApiModelProperty(value = "实例ID：获取解密详情使用", name = "dbInstanceId")
    private Long dbInstanceId;

    @ApiModelProperty(value = "表名称：获取解密详情使用", name = "tableName")
    private String tableName;

    @ApiModelProperty(value = "状态", name = "status",example = "0:已配置 1:未配置")
    private String status;


}
