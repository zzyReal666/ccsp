package com.spms.dbhsm.stockDataProcess.domain.dto;

import lombok.Data;

/**
 * @author zzypersonally@gmail.com
 * @description 新增临时字段信息
 * @since 2024/4/29 16:26
 */
@Data
public class AddColumnsDTO {

    //字段名  注意，此处为原始字段，即待加密的字段，临时字段的前后缀会在具体实现中添加
    private String columnName;

    //字段备注
    private String comment;

    //是否为空 true:非空 false:可空
    private boolean notNull;
}
