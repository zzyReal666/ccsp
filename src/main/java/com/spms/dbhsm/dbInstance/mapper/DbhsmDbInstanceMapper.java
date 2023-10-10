package com.spms.dbhsm.dbInstance.mapper;

import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.domain.VO.InstanceServerNameVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据库实例Mapper接口
 *
 * @author spms
 * @date 2023-09-19
 */
@Mapper
public interface DbhsmDbInstanceMapper
{
    /**
     * 查询数据库实例
     *
     * @param id 数据库实例主键
     * @return 数据库实例
     */
    public DbhsmDbInstance selectDbhsmDbInstanceById(Long id);

    /**
     * 查询数据库实例列表
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 数据库实例集合
     */
    public List<DbhsmDbInstance> selectDbhsmDbInstanceList(DbhsmDbInstance dbhsmDbInstance);

    /**
     * 新增数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    public int insertDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance);

    /**
     * 修改数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    public int updateDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance);

    /**
     * 删除数据库实例
     *
     * @param id 数据库实例主键
     * @return 结果
     */
    public int deleteDbhsmDbInstanceById(Long id);

    /**
     * 批量删除数据库实例
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmDbInstanceByIds(Long[] ids);

    List<InstanceServerNameVO> listDbInstanceSelect(InstanceServerNameVO instanceServerNameVO);

    //DbhsmDbInstance selectDbhsmDbInstanceByUserId(Long id);
}
