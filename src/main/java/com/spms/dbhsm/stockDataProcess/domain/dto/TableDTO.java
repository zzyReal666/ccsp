package com.spms.dbhsm.stockDataProcess.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * @author zzypersonally@gmail.com
 * @description   待加密的表的信息
 * @since 2024/4/28 15:43
 */
@Data
public class TableDTO {

    /**
     * id
     */
    private Long id;

    /**
     * 表名字
     */
    private String tableName;

    /**
     * 线程条数
     */
    private Integer threadNum;

    /**
     * 每批/每个块的条数
     */
    private Integer batchSize;

    /**
     *  待加密的字段/列信息
     */
    List<ColumnDTO> columnDTOList;

}
