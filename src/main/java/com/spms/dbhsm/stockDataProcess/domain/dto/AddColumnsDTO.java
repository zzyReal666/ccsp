package com.spms.dbhsm.stockDataProcess.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author zzypersonally@gmail.com
 * @description 新增临时字段信息
 * @since 2024/4/29 16:26
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddColumnsDTO {

    //加密 还是 解密 true:加密 false:解密
    private boolean encrypt;

    //字段名  注意，此处为原始字段，即待加密的字段，临时字段的前后缀会在具体实现中添加
    private String columnName;

    //字段备注
    private String comment;

    //是否为空 true:非空 false:可空
    private boolean notNull;

    /**
     * 列原始定义 ,具体的键值对取决于具体的数据库，例如ClickHouse需要的属性，其键从ClickHouseExecute的声明的静态变量中获取
     * mysql: type
     * postgres: type
     */
    private Map<String, String> columnDefinition;

}
