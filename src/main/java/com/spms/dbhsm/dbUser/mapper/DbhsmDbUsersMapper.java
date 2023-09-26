package com.spms.dbhsm.dbUser.mapper;

import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据库用户Mapper接口
 *
 * @author ccsp
 * @date 2023-09-25
 */
@Mapper
public interface DbhsmDbUsersMapper
{
    /**
     * 查询数据库用户
     *
     * @param id 数据库用户主键
     * @return 数据库用户
     */
    public DbhsmDbUser selectDbhsmDbUsersById(Long id);

    /**
     * 查询数据库用户列表
     *
     * @param dbhsmDbUser 数据库用户
     * @return 数据库用户集合
     */
    public List<DbhsmDbUser> selectDbhsmDbUsersList(DbhsmDbUser dbhsmDbUser);

    /**
     * 新增数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    public int insertDbhsmDbUsers(DbhsmDbUser dbhsmDbUser);

    /**
     * 修改数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    public int updateDbhsmDbUsers(DbhsmDbUser dbhsmDbUser);

    /**
     * 删除数据库用户
     *
     * @param id 数据库用户主键
     * @return 结果
     */
    public int deleteDbhsmDbUsersById(Long id);

    /**
     * 批量删除数据库用户
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmDbUsersByIds(Long[] ids);
}
