package com.spms.dbhsm.taskQueue.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-22 08:52 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */


@Data
public class TaskQueueRequest {

    @ApiModelProperty(name = "队列ID")
    private Long taskId;

    @ApiModelProperty(value = "taskMode", example = "enc:加密、dec:解密")
    private String taskMode;

    @ApiModelProperty(value = "taskType", example = "down:暂停、up:启动")
    private String taskType;

}
