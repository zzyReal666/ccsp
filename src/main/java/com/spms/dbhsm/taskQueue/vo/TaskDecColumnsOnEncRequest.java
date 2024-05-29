package com.spms.dbhsm.taskQueue.vo;

import com.spms.dbhsm.encryptcolumns.vo.EncryptColumns;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-29 09:45 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class TaskDecColumnsOnEncRequest {

    @ApiModelProperty(name = "batchCount",value = "批次条数")
    private Integer batchCount;

    @ApiModelProperty(name = "threadCount",value = "线程数")
    private Integer threadCount;

    @ApiModelProperty(name = "taskId",value = "队列ID")
    private Long taskId;

    @ApiModelProperty(name = "encryptedLists",value = "加密列")
    private List<EncryptColumns> encryptedLists;
}
