package com.spms.dbhsm.encryptcolumns.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-16 13:38 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class EncryptColumns {

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

    //详情使用
    private Integer encryptionStatus;
}
