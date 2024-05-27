package com.spms.dbhsm.encryptcolumns.mapper;

import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据库加密列Mapper接口
 * 
 * @author diq
 * @date 2023-09-27
 */
@Mapper
public interface DbhsmEncryptColumnsMapper
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
    public List<DbhsmEncryptColumns> selectDbhsmEncryptColumnsList(DbhsmEncryptColumns dbhsmEncryptColumns);

    /**
     * 新增数据库加密列
     * 
     * @param dbhsmEncryptColumns 数据库加密列
     * @return 结果
     */
    public int insertDbhsmEncryptColumns(DbhsmEncryptColumns dbhsmEncryptColumns);

    /**
     * 修改数据库加密列
     * 
     * @param dbhsmEncryptColumns 数据库加密列
     * @return 结果
     */
    public int updateDbhsmEncryptColumns(DbhsmEncryptColumns dbhsmEncryptColumns);

    /**
     * 删除数据库加密列
     * 
     * @param id 数据库加密列主键
     * @return 结果
     */
    public int deleteDbhsmEncryptColumnsById(String id);

    /**
     * 批量删除数据库加密列
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmEncryptColumnsByIds(String[] ids);

    /**
     * 根据实例ID和列名称获取加密队列信息
     *
     * @param dbInstanceId 实例
     * @param columns 列信息
     * @return 结果
     */
    List<DbhsmEncryptColumns> selectDbhsmEncryptBydbInstanceColumnsList(String dbInstanceId,List<String> columns);

    void deleteByEncryptColumnsOnTable(@Param("tableId") String tableId);
}
