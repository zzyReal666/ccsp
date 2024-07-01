package com.spms.dbhsm.taskQueue.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-22 11:40 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class TaskQueueListResponse {

    @ApiModelProperty(name = "userName",value = "数据库用户")
    private String userName;

    @ApiModelProperty(name = "dbInstanceId",value = "实例ID")
    private String dbInstanceId;

    @ApiModelProperty(name = "taskId",value = "队列ID")
    private String taskId;

    @ApiModelProperty(name = "databaseCapitalName",value = "资产名")
    private String databaseCapitalName;

    @ApiModelProperty(name = "databaseDba",value = "数据库")
    private String databaseDba;

    @ApiModelProperty(name = "databaseType",value = "类型")
    private String databaseType;

    @ApiModelProperty(name = "databaseServerName",value = "实例名")
    private String databaseServerName;

    @ApiModelProperty(name = "databaseEdition",value = "版本")
    private String databaseEdition;

    @ApiModelProperty(name = "databaseIp",value = "IP地址")
    private String databaseIp;

    @ApiModelProperty(name = "databasePort",value = "端口")
    private String databasePort;

    @ApiModelProperty(name = "tableId",value = "表ID")
    private Long tableId;

    @ApiModelProperty(name = "tableName",value = "表名")
    private String tableName;

    @ApiModelProperty(name = "finishedColumns",value = "被加密列(已完成)")
    private String finishedColumns;

    @ApiModelProperty(name = "unfinishedColumns",value = "被加密列(未完成)")
    private String unfinishedColumns;

    @ApiModelProperty(name = "encStatus",value = "加密列状态")
    private Integer encStatus;

    @ApiModelProperty(name = "decStatus",value = "解密列状态")
    private Integer decStatus;

    @ApiModelProperty(name = "tableStatus",value = "表状态")
    private Integer tableStatus;

    @ApiModelProperty(name = "batchCount",value = "批次条数")
    private Integer batchCount;

    @ApiModelProperty(name = "threadCount",value = "线程数")
    private Integer threadCount;
}
