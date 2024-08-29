package com.spms.dbhsm.taskQueue.vo;

import com.spms.dbhsm.encryptcolumns.vo.EncryptColumns;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-21 11:58 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class TaskQueueInsertRequest {

    @ApiModelProperty(name = "instanceId",value = "数据库实例ID")
    private Long instanceId;

    @ApiModelProperty(name = "databaseServerName",value = "数据库名")
    private String databaseServerName;

    @ApiModelProperty(name = "tableName",value = "表名")
    private String tableName;

    @ApiModelProperty(name = "batchCount",value = "批次条数")
    private Integer batchCount;

    @ApiModelProperty(name = "threadCount",value = "线程数")
    private Integer threadCount;

    @ApiModelProperty(name = "tableId",value = "表ID",notes = "新增解密队列使用")
    private String tableId;

    @ApiModelProperty(name = "userName", value = "用户名称")
    private String userName;

    @ApiModelProperty(name = "encryptedLists", value = "加密列数据")
    private List<EncryptColumns> encryptedLists;

}
