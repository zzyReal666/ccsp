package com.spms.dbhsm.dbUser.service;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;

import java.sql.SQLException;
import java.util.List;

/**
 * 数据库用户Service接口
 *
 * @author ccsp
 * @date 2023-09-25
 */

public interface IDbhsmDbUsersService
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
    public int insertDbhsmDbUsers(DbhsmDbUser dbhsmDbUser) throws Exception;

    /**
     * 修改数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    public int updateDbhsmDbUsers(DbhsmDbUser dbhsmDbUser);

    /**
     * 批量删除数据库用户
     *
     * @param ids 需要删除的数据库用户主键集合
     * @return 结果
     */
    public int deleteDbhsmDbUsersByIds(Long[] ids) throws SQLException, ZAYKException;

    /**
     * 删除数据库用户信息
     *
     * @param id 数据库用户主键
     * @return 结果
     */
    public int deleteDbhsmDbUsersById(Long id);

    AjaxResult2 treeData();

    String dmPwdPolicyValidate(DbhsmDbUser dbhsmDbUser);
}
