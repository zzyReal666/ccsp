package com.spms.dbhsm.encryptcolumns.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-15 15:48 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class UpEncryptColumnsRequest {

    @ApiModelProperty(name = "taskId",value = "任务队列id")
    private Long taskId;

    @ApiModelProperty(name = "dbInstanceId",value = "数据库实例ID")
    private String dbInstanceId;

    @ApiModelProperty(name = "batchCount",value = "每批条数")
    private Integer batchCount;

    @ApiModelProperty(name = "threadCount",value = "线程数")
    private Integer threadCount;

    @ApiModelProperty(name = "list", value = "加密列数据")
    private List<EncryptColumns> list;


}
