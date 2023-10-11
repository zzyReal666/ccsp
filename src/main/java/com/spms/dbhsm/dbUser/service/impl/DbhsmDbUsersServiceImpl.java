package com.spms.dbhsm.dbUser.service.impl;

import com.ccsp.common.core.constant.SecurityConstants;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.tree.EleTreeWrapper;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.system.api.systemApi.RemoteDicDataService;
import com.ccsp.system.api.systemApi.domain.SysDictData;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.ProcedureUtil;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbOracleInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.domain.DbhsmUserPermissionGroup;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import com.spms.dbhsm.dbUser.mapper.DbhsmUserPermissionGroupMapper;
import com.spms.dbhsm.dbUser.service.IDbhsmDbUsersService;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionGroup;
import com.spms.dbhsm.permissionGroup.domain.dto.PermissionGroupForUserDto;
import com.spms.dbhsm.permissionGroup.mapper.DbhsmPermissionGroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

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
    DbhsmUserPermissionGroupMapper dbhsmUserPermissionGroupMapper;

    @Autowired
    DbhsmEncryptColumnsMapper dbhsmEncryptColumnsMapper;

    @Autowired
    RemoteDicDataService remoteDicDataService;

    @Autowired
    private DbhsmDbInstanceMapper instanceMapper;
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
        List<DbhsmDbUser> dbhsmDbUsersResult = new ArrayList<>();
        Connection conn = null;
        //查询所有的数据库实例
        DbhsmDbInstance dbhsmDbInstance = new DbhsmDbInstance();
        if (dbhsmDbUser.getDatabaseInstanceId() != null) {
            dbhsmDbInstance.setId(dbhsmDbUser.getDatabaseInstanceId());
        }
        if(dbhsmDbUser.getDatabaseType() != null){
            dbhsmDbInstance.setDatabaseType(dbhsmDbUser.getDatabaseType());
        }
        List<DbhsmDbInstance> instancesList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(dbhsmDbInstance);
        //查询通过web界面创建的所有用户,用于区分哪些属于通过web创建的用户
        List<DbhsmDbUser> dbhsmDbUsers = dbhsmDbUsersMapper.selectDbhsmDbUsersList(dbhsmDbUser);
        //根据用户与数据库实例的对应关系表，把实例DbInstanceGetConnDTO赋值给用户。
        dbhsmDbUsers.forEach(dbUser -> {
            DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbUser.getDatabaseInstanceId());
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
                    String sql = "";
                    String dbType = instance.getDatabaseType();
                    switch (dbType){
                        case DbConstants.DB_TYPE_ORACLE:
                            sql = DbConstants.DB_SQL_ORACLE_USER_QUERY;
                            break;
                        case DbConstants.DB_TYPE_SQLSERVER:
                            sql = DbConstants.DB_SQL_SQLSERVER_USER_QUERY;
                            break;
                        case  DbConstants.DB_TYPE_MYSQL:
                            sql = DbConstants.DB_SQL_MYSQL_USER_QUERY;
                            break;
                        default:
                            throw new ZAYKException("暂不支持的数据库类型");
                    }
                    preparedStatement = conn.prepareStatement(sql);
                    resultSet = preparedStatement.executeQuery();
                    //对返回结果进行封装用户对象
                    while (resultSet.next()) {
                        DbhsmDbUser dbUser = getDbUser(resultSet, instance, connDTO, dbhsmDbUsers);
                        dbhsmDbUsersResult.add(dbUser);
                    }
                }
            } catch (ZAYKException | SQLException | ParseException e) {
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
        return dbhsmDbUsersResult.stream().sorted(Comparator.comparing(DbhsmDbUser::getIsSelfBuilt)).collect(Collectors.toList());
    }

    private DbhsmDbUser getDbUser(ResultSet resultSet, DbhsmDbInstance instance, DbInstanceGetConnDTO connDTO, List<DbhsmDbUser> dbhsmDbUsers) throws SQLException, ZAYKException, ParseException {
        DbhsmDbUser dbUser = new DbhsmDbUser();
        String dbType = instance.getDatabaseType();
        switch (dbType) {
            case DbConstants.DB_TYPE_ORACLE:
                dbUser.setUserName(resultSet.getString("userName"));
                dbUser.setUserId(resultSet.getString("user_id"));
                dbUser.setCreated(resultSet.getDate("created"));
                dbUser.setCommon(resultSet.getString("common"));
                dbUser.setAllShard(resultSet.getString("all_shard"));
                dbUser.setDefaultCollation(resultSet.getString("default_collation"));
                dbUser.setInherited(resultSet.getString("inherited"));
                dbUser.setOracleMaintained(resultSet.getString("oracle_maintained"));
                dbUser.setImplicit(resultSet.getString("implicit"));
                break;
            case DbConstants.DB_TYPE_SQLSERVER:
                dbUser.setUserName(resultSet.getString("name"));
                dbUser.setUserId(resultSet.getString("principal_id"));
                dbUser.setCreated(resultSet.getDate("create_date"));
                break;
            case DbConstants.DB_TYPE_MYSQL:
                dbUser.setUserName(resultSet.getString("User"));
                break;
            default:
                throw new ZAYKException("数据库类型不支持");
        }
        dbUser.setSecretService(instance.getSecretService());
        dbUser.setPermissionGroupForUserDto(new PermissionGroupForUserDto());
        connDTO.setDatabaseDbaPassword(null);
        dbUser.setDbInstanceGetConnDTO(connDTO);
        dbUser.setIsSelfBuilt(DbConstants.IS_NOT_SELF_BUILT);
        //遍历数据库中的用户，如果和查询出的用户名一致说明为web端创建，设置其is_self_built属性值为0
        for (DbhsmDbUser user : dbhsmDbUsers) {
            //从被管理的数据库中查询的用户信息与管理端数据库中的用户信息进匹配
            String userName = user.getUserName();
            if (instance.getDatabaseType().equals(DbConstants.DB_TYPE_ORACLE)) {
                userName = user.getUserName().toUpperCase();
            }
            if (dbUser.getUserName().equals(userName) && instance.getId().equals(user.getDatabaseInstanceId())) {
                dbUser.setIsSelfBuilt(DbConstants.IS_SELF_BUILT);
                DbhsmPermissionGroup permissionGroup = dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupByPermissionGroupId(user.getPermissionGroupId());
                PermissionGroupForUserDto permissionGroupForUser = new PermissionGroupForUserDto();
                BeanUtils.copyProperties(permissionGroup, permissionGroupForUser);
                dbUser.setPermissionGroupForUserDto(permissionGroupForUser);
                dbUser.setId(user.getId());
            }
        }
        return dbUser;
    }

    /**
     * 新增数据库用户
     *
     * @param dbhsmDbUser 数据库用户
     * @return 结果
     */
    @Override
    public int insertDbhsmDbUsers(DbhsmDbUser dbhsmDbUser) throws ZAYKException, SQLException {
        //根据实例id获取数据库实例
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbhsmDbUser.getDatabaseInstanceId());
        if (instance == null) {
            log.info("数据库实例不存在！");
            return 0;
        }
        String dbType = instance.getDatabaseType();
        switch (dbType) {
            case DbConstants.DB_TYPE_ORACLE:
                return insertOracleUser(dbhsmDbUser, instance);
            case DbConstants.DB_TYPE_SQLSERVER:
                return insertSqlServerlUser(dbhsmDbUser, instance);
            case DbConstants.DB_TYPE_MYSQL:
                return insertMysqlUser(dbhsmDbUser, instance);
            default:
                log.info("数据库类型不支持！");
                throw new ZAYKException("数据库类型不支持！");
        }
    }

    int insertMysqlUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws ZAYKException {
        String sqlCreateUser,username, password, sql;
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        Long permissionGroupId = dbhsmDbUser.getPermissionGroupId();
        int executeUpdate = 0;
        //根据实例获取数据库连接
        try {
            connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            if (Optional.ofNullable(connection).isPresent()) {
                username = dbhsmDbUser.getUserName();
                password = dbhsmDbUser.getPassword();
                //创建用户sql
                sqlCreateUser = "CREATE USER '" + username + "' @'%' IDENTIFIED BY '" + password + "'";
                preparedStatement = connection.prepareStatement(sqlCreateUser);
                boolean execute = preparedStatement.execute();
                if(!execute){
                    int result = insertDbUsers(dbhsmDbUser);
                    if (result != 1) {
                        log.error("创建用户失败!");
                        throw new ZAYKException("创建用户失败!");
                    }
                }
                //根据权限组id查询权限组对应的所有权限SQL：
                List<String> permissionsSqlList = dbhsmPermissionGroupMapper.getPermissionsSqlByPermissionsGroupid(permissionGroupId);
                //赋权
                for (int i = 0; i < permissionsSqlList.size(); i++) {
                    if (!(permissionsSqlList.get(i).toLowerCase().startsWith("grant") && !(permissionsSqlList.get(i).toLowerCase().startsWith("revoke")))) {
                        log.info("不支持的授权SQL:" + permissionsSqlList.get(i));
                        throw new ZAYKException("不支持的授权SQL:" + permissionsSqlList.get(i));
                    }
                    sql = permissionsSqlList.get(i).trim() + " on " + instance.getDatabaseServerName() + ".* to '" + username + "'@'%'";
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.executeUpdate();
                }
                sql ="FLUSH PRIVILEGES;";
                preparedStatement = connection.prepareStatement(sql);
                executeUpdate = preparedStatement.executeUpdate();
                //try {
                //    //创建加解密方法
                //
                //
                //} catch (SQLException e) {
                //    e.printStackTrace();
                //}

            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new ZAYKException(e.getMessage());
        } finally {
            //释放资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return 1;

    }

    int insertSqlServerlUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws ZAYKException {
        String sqlCreateLoginName, sqlCreateUser, sqlEmpowerment;
        String username;
        String password;
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        //根据实例获取数据库连接
        try {
            connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            if (Optional.ofNullable(connection).isPresent()) {
                username = dbhsmDbUser.getUserName();
                password = dbhsmDbUser.getPassword();
                //创建登录名sql
                String dbName = instance.getDatabaseServerName();
                sqlCreateLoginName = "USE " + dbName + ";CREATE LOGIN " + username + " WITH PASSWORD = '" + password + "',default_database=" + dbName;
                //创建用户sql
                sqlCreateUser = "CREATE USER " + username + " FOR LOGIN " + username + " with default_schema=dbo";
                preparedStatement = connection.prepareStatement(sqlCreateLoginName);
                boolean execute1 = preparedStatement.execute();
                preparedStatement = connection.prepareStatement(sqlCreateUser);
                boolean execute = preparedStatement.execute();
                if (!execute) {
                    int result = insertDbUsers(dbhsmDbUser);
                    if (result != 1) {
                        log.error("创建用户失败!");
                        throw new ZAYKException("创建用户失败!");
                    }
                }
                //赋权
                sqlEmpowerment = "exec sp_addrolemember 'db_datareader','" + username + "'";
                preparedStatement = connection.prepareStatement(sqlEmpowerment);
                preparedStatement.execute();
                connection.commit();

                sqlEmpowerment = "exec sp_addrolemember 'db_datawriter','" + username + "'";
                preparedStatement = connection.prepareStatement(sqlEmpowerment);
                preparedStatement.execute();
                connection.commit();

                sqlEmpowerment = "exec sp_addrolemember 'db_ddladmin','" + username + "'";
                preparedStatement = connection.prepareStatement(sqlEmpowerment);
                connection.commit();

                sqlEmpowerment = "ALTER DATABASE [" + instance.getDatabaseServerName() + "] SET TRUSTWORTHY ON;";
                preparedStatement = connection.prepareStatement(sqlEmpowerment);
                preparedStatement.execute();
                connection.commit();


                try {
                    ProcedureUtil.transSQLServerAssembly(connection, instance.getDatabaseServerName(), dbhsmDbUser.getEncLibapiPath());
                    connection.commit();

                    //创建加解密方法
                    ProcedureUtil.transSQLServerStringEncrypt(connection);
                    ProcedureUtil.transSQLServerStringDecrypt(connection);
                    connection.commit();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                sqlEmpowerment = "USE " + dbName + "; GRANT REFERENCES ON ASSEMBLY::libsqlextdll TO " + username;
                preparedStatement = connection.prepareStatement(sqlEmpowerment);

                log.info(sqlEmpowerment);
                sqlEmpowerment = "USE " + dbName + " ;GRANT EXECUTE ON dbo.func_string_encrypt TO " + username;
                preparedStatement = connection.prepareStatement(sqlEmpowerment);
                log.info(sqlEmpowerment);
                sqlEmpowerment = "USE " + dbName + " ;GRANT EXECUTE ON dbo.func_string_decrypt TO " + username;
                preparedStatement = connection.prepareStatement(sqlEmpowerment);
                log.info(sqlEmpowerment);
                connection.commit();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new ZAYKException(e.getMessage());
        } finally {
            //释放资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return 1;
    }

    int insertOracleUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws ZAYKException, SQLException {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        int executeUpdate = 0;
        String username, password, tableSpace, sql;
        int result = insertDbUsers(dbhsmDbUser);
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
                ProcedureUtil.cOciTransStringEncrypt(conn, instance.getDatabaseDba(), username);
                //FPE加密
                ProcedureUtil.cOciTransFPEEncrypt(conn, instance.getDatabaseDba(), username);
                //解密
                ProcedureUtil.cOciTransStringDecryptP(conn, instance.getDatabaseDba(), username);
                ProcedureUtil.cOciTransStringDecryptF(conn, username);
                ProcedureUtil.cOciTransFpeDecryptF(conn, username);
                //FPE解密
                ProcedureUtil.cOciTransFPEDecrypt(conn, instance.getDatabaseDba(), username);
                conn.setAutoCommit(false);
                conn.commit();

                //赋执行库文件liboraextapi的权限
                sql = "CREATE OR REPLACE LIBRARY liboraextapi AS '" + dbhsmDbUser.getEncLibapiPath() + "'";
                log.info("赋执行库文件liboraextapi的权限sql: {}", sql);
                preparedStatement = conn.prepareStatement(sql);
                preparedStatement.execute();

                sql = "grant execute on liboraextapi to " + username;
                log.info("赋执行库文件liboraextapi的权限给用户 sql: {}", sql);
                preparedStatement = conn.prepareStatement(sql);
                preparedStatement.execute();
            }
        } catch (ZAYKException e) {
            e.printStackTrace();
            throw new ZAYKException(e.getErrMsg());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException(e);
        } finally {
            //释放资源
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
        return executeUpdate == 0 ? 1 : executeUpdate;
    }

    private void destroyUserConnPool(DbhsmDbInstance instance, String username) {
        //释放用户资源（userConn、userStatement）销毁用户连接池
        DbOracleInstancePoolKeyDTO instancePoolKeyDTO = new DbOracleInstancePoolKeyDTO();
        BeanUtils.copyProperties(instance, instancePoolKeyDTO);
        instancePoolKeyDTO.setDatabaseDba(username);
        DbConnectionPoolFactory.getInstance().unbind(instancePoolKeyDTO);
    }

    private int insertDbUsers(DbhsmDbUser dbhsmDbUser) {
        //标记为web端创建的用户
        dbhsmDbUser.setIsSelfBuilt(DbConstants.CREATED_ON_WEB_SEDE);
        //标记为创建的是普通用户
        dbhsmDbUser.setUserRole(DbConstants.ORDINARY_USERS);
        dbhsmDbUser.setCreated(new Date());
        dbhsmDbUsersMapper.insertDbhsmDbUsers(dbhsmDbUser);
        //新增用户与权限组对应关系
        DbhsmUserPermissionGroup permissionGroup = new DbhsmUserPermissionGroup();
        permissionGroup.setUserId(dbhsmDbUser.getId());
        permissionGroup.setPermissionGroupId(dbhsmDbUser.getPermissionGroupId());
        int i = dbhsmUserPermissionGroupMapper.insertDbhsmUserPermissionGroup(permissionGroup);
        return i;
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
    @Transactional(rollbackFor = SQLException.class)
    public int deleteDbhsmDbUsersByIds(Long[] ids) throws SQLException, ZAYKException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        int resultSet = 0;
        String sql = "";
        for (Long id : ids) {
            //根据用户id查询用户
            DbhsmDbUser dbhsmDbUser = dbhsmDbUsersMapper.selectDbhsmDbUsersById(id);
            //根据用户id查询用户在哪个实例下
            DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbhsmDbUser.getDatabaseInstanceId());
            //删除用户
            if (ObjectUtils.isEmpty(instance)) {
                throw new ZAYKException("删除数据库用户失败,该用户所属实例不存在");
            }
            //判断用户下的表是否已有加密列配置
            checkEncColConf(dbhsmDbUser.getUserName(), instance.getId());
            resultSet = deleteDbhsmDbUsersById(id);
            if (resultSet != 1) {
                throw new ZAYKException("删除数据库用户失败");
            }
            //根据实例获取连接
            String userName = dbhsmDbUser.getUserName();
            try {
                connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
                //根据连接删除用户
                if (Optional.ofNullable(connection).isPresent()) {
                    sql = "drop user " + userName;
                    if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
                        sql += " cascade";
                        //销毁用户连接池
                        destroyUserConnPool(instance,userName);
                    } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
                        sql += ";drop login " + userName;
                    } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
                        sql = "drop user '" + userName + "'@'%'";
                    }
                    log.info("删除数据库用户执行SQL:{}", sql);
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.execute();
                    connection.commit();
                }
            } catch (SQLException e) {
                log.info("删除数据库用户失败!执行SQL:{}", sql);
                e.printStackTrace();
                throw new SQLException(e.getMessage());
            } finally {
                //释放资源
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return resultSet;
    }

    private void checkEncColConf(String userName, Long instanceId) throws ZAYKException {
        DbhsmEncryptColumns encryptColumns = new DbhsmEncryptColumns();
        encryptColumns.setDbUserName(userName);
        encryptColumns.setDbInstanceId(instanceId);
        List<DbhsmEncryptColumns> encryptColumnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(encryptColumns);
        if(!CollectionUtils.isEmpty(encryptColumnsList)){
            String dbUserNameStr = encryptColumnsList.stream().map(DbhsmEncryptColumns::getDbTable).distinct().collect(Collectors.joining(","));
            throw new ZAYKException("该用户下的表:"+dbUserNameStr+"已配置加密列，不允许删除！");
        }
    }

    public static void main(String[] args) {
        DbhsmEncryptColumns encryptColumns = new DbhsmEncryptColumns();
        encryptColumns.setDbUserName("dbhsm_user");
        DbhsmEncryptColumns encryptColumns2 = new DbhsmEncryptColumns();
        encryptColumns2.setDbUserName("dbhsm_user");
        List<DbhsmEncryptColumns> list =new ArrayList<>();
        list.add(encryptColumns);
        //list.add(encryptColumns2);
        String collect = list.stream().map(DbhsmEncryptColumns::getDbUserName).collect(Collectors.joining(","));
        System.out.println(collect);
    }
    /**
     * 删除数据库用户信息
     *
     * @param id 数据库用户主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbUsersById(Long id) {
        //删除用户与权限组关系
        dbhsmUserPermissionGroupMapper.deleteDbhsmUserPermissionGroupByUserId(id);
        return dbhsmDbUsersMapper.deleteDbhsmDbUsersById(id);
    }

    @Override
    public AjaxResult2 treeData() {
        List<DbhsmDbUser> usersList = new ArrayList<>();
        SysDictData sysDictData;
        DbhsmDbUser user;
        String tableName;
        Connection conn = null;
        List<Map<String, Object>> instancetTrees = new ArrayList<Map<String, Object>>();
        List<SysDictData> dbTypeDictData = remoteDicDataService.romoteDictType(DbConstants.DBHSM_DB_TYPE, SecurityConstants.INNER).getData();

        for (int i = 0; i < dbTypeDictData.size(); i++) {
            sysDictData = dbTypeDictData.get(i);
            try {
                Map<String, Object> dbTypeMap = new HashMap<String, Object>();
                dbTypeMap.put("id", sysDictData.getDictCode());
                dbTypeMap.put("pId", "0");
                dbTypeMap.put("title", sysDictData.getDictLabel());
                dbTypeMap.put("level", 1);
                dbTypeMap.put("dbType", sysDictData.getDictValue());
                instancetTrees.add(dbTypeMap);

                List<DbhsmDbInstance> instanceList = instanceMapper.selectDbhsmDbInstanceList(new DbhsmDbInstance());
                for (int j = 0; j < instanceList.size(); j++) {
                    DbhsmDbInstance instance = instanceList.get(j);
                    if(sysDictData.getDictValue().equals(instance.getDatabaseType())){
                        Map<String, Object> instanceMap = new HashMap<String, Object>();
                        instanceMap.put("id", instance.getId());
                        instanceMap.put("pId", sysDictData.getDictCode());
                        instanceMap.put("title", instance.getDatabaseServerName());
                        instanceMap.put("level", 2);
                        instancetTrees.add(instanceMap);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (StringUtils.isEmpty(instancetTrees)) {
            return AjaxResult2.success();
        }
        return AjaxResult2.success(EleTreeWrapper.getInstance().getTree(instancetTrees, "pId", "id"));
    }
}
