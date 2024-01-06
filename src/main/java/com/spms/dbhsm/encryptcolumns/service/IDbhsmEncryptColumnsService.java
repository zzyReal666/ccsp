package com.spms.dbhsm.encryptcolumns.service;


import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.system.api.systemApi.domain.SysDictData;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsDto;

import java.sql.SQLException;
import java.util.List;

/**
 * 数据库加密列Service接口
 *
 * @author diq
 * @date 2023-09-27
 */
public interface IDbhsmEncryptColumnsService
{
    /**
     * 查询数据库加密列
     *
     * @param id 数据库加密列主键
     * @return 数据库加密列
     */
    public DbhsmEncryptColumns selectDbhsmEncryptColumnsById(String id);

    /**
     * 查询数据库加密列列表
     *
     * @param dbhsmEncryptColumns 数据库加密列
     * @return 数据库加密列集合
     */
    public List<DbhsmEncryptColumns> selectDbhsmEncryptColumnsList(DbhsmEncryptColumnsDto dbhsmEncryptColumns) throws Exception;

    /**
     * 新增数据库加密列
     *
     * @param dbhsmEncryptColumns 数据库加密列
     * @return 结果
     */
    public int insertDbhsmEncryptColumns(DbhsmEncryptColumnsAdd dbhsmEncryptColumns) throws Exception;

    /**
     * 修改数据库加密列
     *
     * @param dbhsmEncryptColumns 数据库加密列
     * @return 结果
     */
    public int updateDbhsmEncryptColumns(DbhsmEncryptColumns dbhsmEncryptColumns);

    /**
     * 批量删除数据库加密列
     *
     * @param ids 需要删除的数据库加密列主键集合
     * @return 结果
     */
    public int deleteDbhsmEncryptColumnsByIds(String[] ids) throws SQLException, Exception;

    /**
     * 删除数据库加密列信息
     *
     * @param id 数据库加密列主键
     * @return 结果
     */
    public int deleteDbhsmEncryptColumnsById(String id);

    /**
     * 树结构
     * @return
     */
    AjaxResult2 treeData();

    List<SysDictData> selectDMAlg(DbhsmEncryptColumnsDto dbhsmEncryptColumns) throws ZAYKException, SQLException;
    String getTaskStatus();
}
