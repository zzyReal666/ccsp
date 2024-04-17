package com.spms.dbhsm.warningFile.service;


import com.spms.dbhsm.warningFile.domain.DbhsmIntegrityFileConfig;

import java.util.List;

/**
 * 文件完整性校验Service接口
 *
 * @author diq
 * @date 2024-04-17
 */
public interface IDbhsmIntegrityFileConfigService {
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
     * 批量删除文件完整性校验
     *
     * @param ids 需要删除的文件完整性校验主键集合
     * @return 结果
     */
    public int deleteDbhsmIntegrityFileConfigByIds(Long[] ids);

    /**
     * 删除文件完整性校验信息
     *
     * @param id 文件完整性校验主键
     * @return 结果
     */
    public int deleteDbhsmIntegrityFileConfigById(Long id);
}
