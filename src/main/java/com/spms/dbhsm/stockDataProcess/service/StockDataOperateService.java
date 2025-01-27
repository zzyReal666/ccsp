package com.spms.dbhsm.stockDataProcess.service;

import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;

/**
 * @author zzypersonally@gmail.com
 * @description 存量数据加密/解密服务
 * @since 2024/4/28 16:34
 */
public interface StockDataOperateService {


    /**
     * 存量数据加密/解密服务
     *
     * @param databaseDTO 数据库信息
     * @param operateType 操作类型 true:加密 false:解密
     */
    void stockDataOperate(DatabaseDTO databaseDTO, boolean operateType) throws Exception;

    /**
     * 暂停
     */
    void pause(String tableId) throws Exception;


    /**
     * 继续
     */
    void resume(String tableId) throws Exception;


    /**
     * 查询进度
     */
    int queryProgress(String tableId) throws Exception;

    /**
     * 清理map  任务完成后调用，此后查询进度先判断状态，已经完成的为100
     */
    void clearMap(String tableId) throws Exception;

}
