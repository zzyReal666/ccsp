package com.spms.dbhsm.taskQueue.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-28 14:32 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */


@Data
public class TaskPolicyDetailsResponse {

    @ApiModelProperty(name = "encryptColumns", value = "加密列名")
    private String encryptColumns;

    @ApiModelProperty(name = "encryptionAlgorithm", value = "加密算法")
    private String encryptionAlgorithm;

    @ApiModelProperty(name = "columnsType", value = "列类型")
    private String columnsType;

    @ApiModelProperty(name = "secretKeyId", value = "密钥ID")
    private String secretKeyId;

    @ApiModelProperty(name = "ethernetPort", value = "下发网口")
    private String ethernetPort;

    @ApiModelProperty(name = "encryptionStatus", value = "加密列状态")
    private Integer encryptionStatus;

    @ApiModelProperty(name = "disablingEncryption", value = "禁止加密原因")
    private String disablingEncryption;
}
