package com.spms.dbhsm.stockDataProcess.domain.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zzypersonally@gmail.com
 * @description 加密列信息
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
     * 数据类型
     */
    private String dataType;


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
    private Map<String, String> property = new HashMap<>();

    /**
     * 辅助查询列参数
     * 参数列表： 可以自由拓展
     * enable：true 使用｜false 不使用 默认不使用
     * encryptor：算法器名字
     * columnName：辅助查询列名字
     */
    private Map<String, Object> assistedQueryProps = new HashMap<>();

    /**
     * 模糊查询列参数
     * 参数列表： 可以自由拓展
     * enable：true 使用｜false 不使用 默认不使用
     * alg：算法
     */
    private Map<String, Object> likeQueryProps = new HashMap<>();
}
