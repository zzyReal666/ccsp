package com.spms.dbhsm.stockDataProcess.domain.dto;

import lombok.Data;

/**
 * @author zzypersonally@gmail.com
 * @description    加密列信息
 * @since 2024/4/28 15:46
 */
@Data
public class ColumnDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 列名
     */
    private String columnName;


    /**
     * 是否非空 true:非空 false:可空
     */
    private boolean notNull;

    /**
     * 注释
     */
    private String comment;

    /**
     * 加密算法
     */
    private String encryptAlgorithm;

    /**
     * 加密密钥索引
     */
    private String encryptKeyIndex;

    /**
     * 加密属性 非必须
     */
    private String property;
}
