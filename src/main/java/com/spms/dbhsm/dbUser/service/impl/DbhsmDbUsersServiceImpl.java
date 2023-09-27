package com.spms.dbhsm.dbUser.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.ProcedureUtil;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import com.spms.dbhsm.dbUser.mapper.DbhsmUserDbInstanceMapper;
import com.spms.dbhsm.dbUser.service.IDbhsmDbUsersService;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionGroup;
import com.spms.dbhsm.permissionGroup.domain.dto.PermissionGroupForUserDto;
import com.spms.dbhsm.permissionGroup.mapper.DbhsmPermissionGroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 数据库用户Service业务层处理
 *
 * @author ccsp
 * @date 2023-09-25
 */
@Service
@Slf4j
public class DbhsmDbUsersServiceImpl implements IDbhsmDbUsersService {
    @Autowired
    private DbhsmDbUsersMapper dbhsmDbUsersMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmPermissionGroupMapper dbhsmPermissionGroupMapper;

    @Autowired
    DbhsmUserDbInstanceMapper dbhsmUserDbInstanceMapper;

    /**
     * 查询数据库用户
     *
     * @param id 数据库用户主键
     * @return 数据库用户
     */
    @Override
    public DbhsmDbUser selectDbhsmDbUsersById(Long id) {
        return dbhsmDbUsersMapper.selectDbhsmDbUsersById(id);
    }

    /**
     * 查询数据库用户列表
     *
     * @param dbhsmDbUser 数据库用户
     * @return 数据库用户
     */
    @Override
    public List<DbhsmDbUser> selectDbhsmDbUsersList(DbhsmDbUser dbhsmDbUser) {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<DbhsmDbUser> dbhsmDbUsersResult = new ArrayList<DbhsmDbUser>();
        Connection conn = null;
        //查询所有的数据库实例
        DbhsmDbInstance dbhsmDbInstance = new DbhsmDbInstance();
        List<DbhsmDbInstance> instancesList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(dbhsmDbInstance);
        //查询通过web界面创建的所有用户,用于区分哪些属于通过web创建的用户
        List<DbhsmDbUser> dbhsmDbUsers = dbhsmDbUsersMapper.selectDbhsmDbUsersList(dbhsmDbUser);
        //根据用户与数据库实例的对应关系表，把实例DbInstanceGetConnDTO赋值给用户。
        dbhsmDbUsers.forEach(dbUser -> {
            DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceByUserId(dbUser.getId());
            if (instance != null) {
                DbInstanceGetConnDTO getConnDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, getConnDTO);
                dbUser.setDbInstanceGetConnDTO(getConnDTO);
            }
        });
        //遍历数据库实例，查看每个实例下的用户
        for (DbhsmDbInstance instance : instancesList) {
            try {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                if (Optional.ofNullable(conn).isPresent()) {
                    //查询数据库实例用户列表
                    String sql = "select  * from all_users where INHERITED='NO'";
                    preparedStatement = conn.prepareStatement(sql);
                    resultSet = preparedStatement.executeQuery();
                    //对返回结果进行封装用户对象
                    while (resultSet.next()) {
                        DbhsmDbUser dbUser = new DbhsmDbUser();
                        dbUser.setUserName(resultSet.getString("userName"));
                        dbUser.setUserId(resultSet.getString("user_id"));
                        dbUser.setCreated(resultSet.getDate("created"));
                        dbUser.setCommon(resultSet.getString("common"));
                        dbUser.setAllShard(resultSet.getString("all_shard"));
                        dbUser.setDefaultCollation(resultSet.getString("default_collation"));
                        dbUser.setInherited(resultSet.getString("inherited"));
                        dbUser.setOracleMaintained(resultSet.getString("oracle_maintained"));
                        dbUser.setImplicit(resultSet.getString("implicit"));
                        dbUser.setSecretService(instance.getSecretService());
                        dbUser.setPermissionGroupForUserDto(new PermissionGroupForUserDto());
                        connDTO.setDatabaseDbaPassword(null);
                        dbUser.setDbInstanceGetConnDTO(connDTO);
                        dbUser.setIsSelfBuilt(DbConstants.IS_NOT_SELF_BUILT);
                        //遍历数据库中的用户，如果和查询出的用户名一致说明为web端创建，设置其is_self_built属性值为0
                        dbhsmDbUsers.stream()
                                .filter(user -> dbUser.getUserName().equals(user.getUserName().toUpperCase()))
                                .findFirst()
                                .ifPresent(user -> {
                                    dbUser.setIsSelfBuilt(DbConstants.IS_SELF_BUILT);
                                    DbhsmPermissionGroup permissionGroup = dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupByPermissionGroupId(user.getPermissionGroupId());
                                    PermissionGroupForUserDto permissionGroupForUser = new PermissionGroupForUserDto();
                                    BeanUtils.copyProperties(permissionGroup, permissionGroupForUser);
                                    dbUser.setPermissionGroupForUserDto(permissionGroupForUser);
                                    dbUser.setId(user.getId());
                                });
                        dbhsmDbUsersResult.add(dbUser);
                    }
                }
            } catch (ZAYKException | SQLException e) {
                e.printStackTrace();
                log.info("获取数据库连接失败！" + e.getMessage());
            }
            //释放资源
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return dbhsmDbUsersResult;
    }

    /**
     * 新增数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDbhsmDbUsers(DbhsmDbUser dbhsmDbUser) throws ZAYKException, SQLException {
        Connection conn = null;
        Connection userConn = null;
        PreparedStatement preparedStatement = null;
        PreparedStatement userStatement = null;
        int executeUpdate = 0;
        String username, password, tableSpace, sql;
        //根据实例id获取数据库实例
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbhsmDbUser.getDatabaseInstanceId());
        if (instance == null) {
            log.info("数据库实例不存在！");
            return 0;
        }
        //标记为web端创建的用户
        dbhsmDbUser.setIsSelfBuilt(DbConstants.CREATED_ON_WEB_SEDE);
        //标记为创建的是普通用户
        dbhsmDbUser.setUserRole(DbConstants.ORDINARY_USERS);
        dbhsmDbUser.setCreated(new Date());
        int result = dbhsmDbUsersMapper.insertDbhsmDbUsers(dbhsmDbUser);
        if (result != 1) {
            log.error("创建用户失败!");
            throw new ZAYKException("创建用户失败!");
        }
        username = dbhsmDbUser.getUserName();
        password = dbhsmDbUser.getPassword();
        tableSpace = dbhsmDbUser.getTableSpace();
        Long permissionGroupId = dbhsmDbUser.getPermissionGroupId();
        try {
            //根据实例获取数据库连接
            conn = DbConnectionPoolFactory.getInstance().getConnection(instance);
            if (Optional.ofNullable(conn).isPresent()) {
                //获取用户创建模式 0：创建无容器数据库用户 1：创建CDB容器中的公共用户
                int userCreateMode = instance.getUserCreateMode();
                if (userCreateMode == DbConstants.USER_CREATE_MODE_CDB) {
                    sql = "CREATE USER c##" + username + " IDENTIFIED BY " + password + " DEFAULT tablespace users";
                } else {
                    sql = "CREATE USER " + username + " IDENTIFIED BY " + password + " DEFAULT TABLESPACE \"" + tableSpace + "\" TEMPORARY TABLESPACE \"TEMP\"";
                }
                preparedStatement = conn.prepareStatement(sql);
                preparedStatement.executeUpdate();
                //根据权限组id查询权限组对应的所有权限SQL：
                List<String> permissionsSqlList = dbhsmPermissionGroupMapper.getPermissionsSqlByPermissionsGroupid(permissionGroupId);
                //赋权
                for (int i = 0; i < permissionsSqlList.size(); i++) {
                    if (!(permissionsSqlList.get(i).toLowerCase().startsWith("grant") && !(permissionsSqlList.get(i).toLowerCase().startsWith("revoke")))) {
                        log.info("不支持的授权SQL:" + permissionsSqlList.get(i));
                        throw new ZAYKException("不支持的授权SQL:" + permissionsSqlList.get(i));
                    }
                    if (permissionsSqlList.get(i).toLowerCase().startsWith("grant")) {
                        sql = permissionsSqlList.get(i).trim() + " to " + username;
                    } else {
                        sql = permissionsSqlList.get(i).trim() + " from " + username;
                    }
                    preparedStatement = conn.prepareStatement(sql);
                    executeUpdate = preparedStatement.executeUpdate();
                }
                //加密
                ProcedureUtil.cOciTransStringEncrypt(conn, username);
                //FPE加密
                ProcedureUtil.cOciTransFPEEncrypt(conn, username);
                //解密
                ProcedureUtil.cOciTransStringDecryptP(conn, username);
                ProcedureUtil.cOciTransStringDecryptF(conn, username);
                ProcedureUtil.cOciTransFpeDecryptF(conn, username);
                //FPE解密
                ProcedureUtil.cOciTransFPEDecrypt(conn, username);
                conn.setAutoCommit(false);
                conn.commit();
                //使用新增的用户创建连接
                DbInstanceGetConnDTO getConnDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, getConnDTO);
                getConnDTO.setDatabaseDba(username);
                getConnDTO.setDatabaseDbaPassword(password);
                userConn = DbConnectionPoolFactory.getInstance().getConnection(getConnDTO);
                if (ObjectUtils.isEmpty(userConn)) {
                    throw new ZAYKException("使用新用户创建连接异常");
                }
                log.info("使用新用户创建连接成功");
                //赋执行库文件liboraextapi的权限
                sql = "CREATE OR REPLACE LIBRARY liboraextapi AS '" + tableSpace + "'";
                log.info("赋执行库文件liboraextapi的权限sql: {}", sql);
                userStatement = userConn.prepareStatement(sql);
                userStatement.execute();
                userConn.commit();
            }
        } catch (ZAYKException e) {
            e.printStackTrace();
            throw new ZAYKException(e.getErrMsg());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException(e);
        } finally {
            //释放资源
            if (userStatement != null) {
                try {
                    userStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (userConn != null) {
                try {
                    userConn.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return executeUpdate;
    }

    /**
     * 修改数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    @Override
    public int updateDbhsmDbUsers(DbhsmDbUser dbhsmDbUser) {
        return dbhsmDbUsersMapper.updateDbhsmDbUsers(dbhsmDbUser);
    }

    /**
     * 批量删除数据库用户
     *
     * @param ids 需要删除的数据库用户主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbUsersByIds(Long[] ids) {
        return dbhsmDbUsersMapper.deleteDbhsmDbUsersByIds(ids);
    }

    /**
     * 删除数据库用户信息
     *
     * @param id 数据库用户主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbUsersById(Long id) {
        return dbhsmDbUsersMapper.deleteDbhsmDbUsersById(id);
    }
}
