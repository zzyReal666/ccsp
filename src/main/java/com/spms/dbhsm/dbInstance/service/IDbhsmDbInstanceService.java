package com.spms.dbhsm.dbInstance.service;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.SelectOption;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.domain.VO.InstanceServerNameVO;

import java.sql.SQLException;
import java.util.List;

/**
 * 数据库实例Service接口
 *
 * @author spms
 * @date 2023-09-19
 */
public interface IDbhsmDbInstanceService
{
    /**
     * 查询数据库实例
     *
     * @param id 数据库实例主键
     * @return 数据库实例
     */
    public DbhsmDbInstance selectDbhsmDbInstanceById(Long id);

    /**
     * 查询数据库实例用户侧边栏使用
     *
     * @param
     * @return 数据库实例
     */
    List<InstanceServerNameVO> listDbInstanceSelect(InstanceServerNameVO instanceServerNameVO);

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
    public int insertDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException, SQLException;

    /**
     * 修改数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    public int updateDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException, SQLException;

    /**
     * 批量删除数据库实例
     *
     * @param ids 需要删除的数据库实例主键集合
     * @return 结果
     */
    public int deleteDbhsmDbInstanceByIds(Long[] ids);

    /**
     * 删除数据库实例信息
     *
     * @param id 数据库实例主键
     * @return 结果
     */
    public int deleteDbhsmDbInstanceById(Long id);

    List<SelectOption>  getDbTablespace(Long id);
}
