package com.spms.dbhsm.taskQueue.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-01-16 09:01 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class DbhsmTaskQueue {

    private static final long serialVersionUID = 1L;

    /**
     * 任务队列id
     */
    @ApiModelProperty(name = "taskId", value = "任务队列id")
    private Integer taskId;

    /**
     * 任务对应表名
     */
    @ApiModelProperty(name = "tableId", value = "任务对应表ID")
    private String tableId;

    /**
     * 加密队列状态：0 未开始1加密中2已暂停3已完成4移除加密队列
     */
    @ApiModelProperty(name = "encStatus", value = "加密队列状态：0 未开始1加密中2已暂停3已完成4移除加密队列")
    private Integer encStatus;

    /**
     * 解密队列状态：0未开始1解密中2已暂停3移除解密队列
     */
    @ApiModelProperty(name = "decStatus", value = "解密队列状态：0未开始1解密中2已暂停3移除解密队列")
    private Integer decStatus;

    /**
     * 创建时间
     */
    @ApiModelProperty(name = "createTime", value = "创建时间")
    private java.util.Date createTime;

    /**
     * 创建者
     */
    @ApiModelProperty(name = "createBy", value = "创建者")
    private String createBy;

    /**
     * 更新时间
     */
    @ApiModelProperty(name = "updateTime", value = "更新时间")
    private java.util.Date updateTime;

    /**
     * 更新者
     */
    @ApiModelProperty(name = "updateBy", value = "更新者")
    private String updateBy;

}
