package com.spms.dbhsm.taskQueue.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-22 11:39 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class TaskQueueListRequest {

    @ApiModelProperty(name = "databaseCapitalName",value = "资产名称")
    private String databaseCapitalName;

    @ApiModelProperty(name = "databaseServerName",value = "数据库名称")
    private String databaseServerName;

    @ApiModelProperty(name = "tableName",value = "表名")
    private String tableName;

    @ApiModelProperty(name = "status",value = "状态")
    private String status;

    private String taskMode;

    //租户模式
    private String createBy;
}
