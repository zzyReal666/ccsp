package com.spms.dbhsm.warningFile.mapper;

import com.spms.dbhsm.warningFile.domain.DbhsmIntegrityFileConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 文件完整性校验Mapper接口
 * 
 * @author diq
 * @date 2024-04-17
 */
@Mapper
public interface DbhsmIntegrityFileConfigMapper 
{
    /**
     * 查询文件完整性校验
     * 
     * @param id 文件完整性校验主键
     * @return 文件完整性校验
     */
    public DbhsmIntegrityFileConfig selectDbhsmIntegrityFileConfigById(Long id);

    /**
     * 查询文件完整性校验列表
     * 
     * @param dbhsmIntegrityFileConfig 文件完整性校验
     * @return 文件完整性校验集合
     */
    public List<DbhsmIntegrityFileConfig> selectDbhsmIntegrityFileConfigList(DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig);

    /**
     * 新增文件完整性校验
     * 
     * @param dbhsmIntegrityFileConfig 文件完整性校验
     * @return 结果
     */
    public int insertDbhsmIntegrityFileConfig(DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig);

    /**
     * 修改文件完整性校验
     * 
     * @param dbhsmIntegrityFileConfig 文件完整性校验
     * @return 结果
     */
    public int updateDbhsmIntegrityFileConfig(DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig);

    /**
     * 删除文件完整性校验
     * 
     * @param id 文件完整性校验主键
     * @return 结果
     */
    public int deleteDbhsmIntegrityFileConfigById(Long id);

    /**
     * 批量删除文件完整性校验
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmIntegrityFileConfigByIds(Long[] ids);
}
