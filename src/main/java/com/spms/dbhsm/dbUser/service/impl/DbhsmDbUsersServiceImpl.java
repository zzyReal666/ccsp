package com.spms.dbhsm.dbUser.service.impl;

import com.ccsp.common.core.constant.SecurityConstants;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.tree.EleTreeWrapper;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.system.api.systemApi.RemoteDicDataService;
import com.ccsp.system.api.systemApi.domain.SysDictData;
import com.spms.common.DMUserPasswordPolicy;
import com.spms.common.constant.DMErrorCode;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.FunctionUtil;
import com.spms.common.dbTool.ProcedureUtil;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbOracleInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbInstance.service.impl.DbhsmDbInstanceServiceImpl;
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
    DbhsmDbInstanceServiceImpl dbhsmDbInstanceService;

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
        if (dbhsmDbUser.getDatabaseType() != null) {
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
                    switch (dbType) {
                        case DbConstants.DB_TYPE_ORACLE:
                            sql = DbConstants.DB_SQL_ORACLE_USER_QUERY;
                            break;
                        case DbConstants.DB_TYPE_SQLSERVER:
                            sql = DbConstants.DB_SQL_SQLSERVER_USER_QUERY;
                            break;
                        case DbConstants.DB_TYPE_MYSQL:
                            sql = DbConstants.DB_SQL_MYSQL_USER_QUERY;
                            break;
                        case DbConstants.DB_TYPE_POSTGRESQL:
                            sql = DbConstants.DB_SQL_POSTGRESQL_USER_QUERY;
                            break;
                        case DbConstants.DB_TYPE_DM:
                            sql = DbConstants.DB_SQL_DM_USER_QUERY;
                            break;
                        default:
                            throw new ZAYKException("暂不支持的数据库类型");
                    }
                    preparedStatement = conn.prepareStatement(sql);
                    resultSet = preparedStatement.executeQuery();
                    //对返回结果进行封装用户对象
                    //id用于排序
                    Long id = 1L;
                    while (resultSet.next()) {
                        DbhsmDbUser dbUser = getDbUser(resultSet, instance, connDTO, dbhsmDbUsers, id);
                        dbhsmDbUsersResult.add(dbUser);
                        id++;
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
        return dbhsmDbUsersResult.stream().sorted(Comparator.comparing(DbhsmDbUser::getIsSelfBuilt).thenComparing(DbhsmDbUser::getId)).collect(Collectors.toList());
    }

    private DbhsmDbUser getDbUser(ResultSet resultSet, DbhsmDbInstance instance, DbInstanceGetConnDTO connDTO, List<DbhsmDbUser> dbhsmDbUsers, Long id) throws SQLException, ZAYKException, ParseException {
        DbhsmDbUser dbUser = new DbhsmDbUser();
        String dbType = instance.getDatabaseType();
        dbUser.setDatabaseType(dbType);
        switch (dbType) {
            case DbConstants.DB_TYPE_ORACLE:
                dbUser.setId(id);
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
                dbUser.setId(id);
                dbUser.setUserName(resultSet.getString("name"));
                dbUser.setUserId(resultSet.getString("principal_id"));
                dbUser.setCreated(resultSet.getDate("create_date"));
                break;
            case DbConstants.DB_TYPE_MYSQL:
                dbUser.setId(id);
                dbUser.setUserName(resultSet.getString("User"));
                break;
            case DbConstants.DB_TYPE_POSTGRESQL:
                dbUser.setId(id);
                dbUser.setUserName(resultSet.getString("usename"));
                break;
            case DbConstants.DB_TYPE_DM:
                dbUser.setId(id);
                dbUser.setUserName(resultSet.getString("userName"));
                dbUser.setUserId(resultSet.getString("user_id"));
                dbUser.setCreated(resultSet.getDate("created"));
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
            if (!user.getDatabaseType().equals(dbUser.getDatabaseType())) {
                continue;
            }
            //从被管理的数据库中查询的用户信息与管理端数据库中的用户信息进匹配
            String userName = user.getUserName();
            if (instance.getDatabaseType().equals(DbConstants.DB_TYPE_ORACLE)) {
                userName = user.getUserName().toUpperCase();
            }
            if (dbUser.getUserName().equals(userName) && instance.getDatabaseType().equals(user.getDatabaseType()) && instance.getId().equals(user.getDatabaseInstanceId())) {
                dbUser.setIsSelfBuilt(DbConstants.IS_SELF_BUILT);
                if (!ObjectUtils.isEmpty(user.getPermissionGroupId())) {
                    DbhsmPermissionGroup permissionGroup = dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupByPermissionGroupId(user.getPermissionGroupId());
                    if (!ObjectUtils.isEmpty(permissionGroup)) {
                        PermissionGroupForUserDto permissionGroupForUser = new PermissionGroupForUserDto();
                        BeanUtils.copyProperties(permissionGroup, permissionGroupForUser);
                        dbUser.setPermissionGroupForUserDto(permissionGroupForUser);
                    }
                }
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
    @Transactional(rollbackFor = Exception.class)
    public int insertDbhsmDbUsers(DbhsmDbUser dbhsmDbUser) throws Exception {
        //根据实例id获取数据库实例
        int i = 0;
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbhsmDbUser.getDatabaseInstanceId());
        if (instance == null) {
            log.info("数据库实例不存在！");
            return 0;
        }
        String dbType = instance.getDatabaseType();
        dbhsmDbUser.setDatabaseType(dbType);
        dbhsmDbUser.setEncLibapiPath(instance.getEncLibapiPath());
        switch (dbType) {
            case DbConstants.DB_TYPE_ORACLE:
                return insertOracleUser(dbhsmDbUser, instance);
            case DbConstants.DB_TYPE_SQLSERVER:
                return insertSqlServerlUser(dbhsmDbUser, instance);
            case DbConstants.DB_TYPE_MYSQL:
                return insertMysqlUser(dbhsmDbUser, instance);
            case DbConstants.DB_TYPE_POSTGRESQL:
                return insertPostgreSqlUser(dbhsmDbUser, instance);
            case DbConstants.DB_TYPE_DM:
                return insertDMUser(dbhsmDbUser, instance);
            default:
                log.info("数据库类型不支持！");
                throw new ZAYKException("数据库类型不支持！");
        }
    }

    @Transactional(rollbackFor = ZAYKException.class)
    public int insertDMUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws Exception {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        int executeUpdate = 0;
        String username, password, tableSpace, sql, permissionsSql = null;
        dbhsmDbUser.setUserName(dbhsmDbUser.getUserName().toUpperCase());
        int result = insertDbUsers(dbhsmDbUser);
        if (result != 1) {
            log.error("创建用户失败!");
            throw new ZAYKException("创建用户失败!");
        }
        username = dbhsmDbUser.getUserName();
        password = dbhsmDbUser.getPassword();
        tableSpace = dbhsmDbUser.getTableSpace();
        Long permissionGroupId = dbhsmDbUser.getPermissionGroupId();
        //根据实例获取数据库连接
        conn = DbConnectionPoolFactory.getInstance().getConnection(instance);
        if (!Optional.ofNullable(conn).isPresent()) {
            throw new ZAYKException("获取数据库连接失败!");
        }
        try {
            sql = "CREATE USER  \"" + username + "\" IDENTIFIED BY \"" + password + "\" DEFAULT TABLESPACE \"" + tableSpace + "\"";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            //释放资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            try {
                conn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            throw new Exception(e.getMessage().split(":")[1]);
        }
        //根据权限组id查询权限组对应的所有权限SQL：
        List<String> permissionsSqlList = dbhsmPermissionGroupMapper.getPermissionsSqlByPermissionsGroupid(permissionGroupId);
        //赋权
        try {
            for (int i = 0; i < permissionsSqlList.size(); i++) {
                permissionsSql = permissionsSqlList.get(i);
                if (StringUtils.isNotEmpty(permissionsSql)) {
                    if (!(permissionsSql.toLowerCase().startsWith("grant") && !(permissionsSql.toLowerCase().startsWith("revoke")))) {
                        log.info("不支持的授权SQL:" + permissionsSql);
                        // 撤销创建的用户
                        sql = "DROP USER IF EXISTS \"" + username + "\"";
                        try {
                            preparedStatement = conn.prepareStatement(sql);
                            preparedStatement.executeUpdate();
                            conn.commit();
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        throw new ZAYKException("不支持的授权SQL:" + permissionsSql);
                    }
                    if (permissionsSql.toLowerCase().startsWith("grant")) {
                        sql = permissionsSql.trim() + " to \"" + username + "\"";
                    } else {
                        sql = permissionsSql.trim() + " from \"" + username + "\"";
                    }
                    preparedStatement = conn.prepareStatement(sql);
                    executeUpdate = preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            String errorCode = e.getMessage().split("\n")[0].split(":")[1];
            if (StringUtils.isNotEmpty(errorCode)) {
                String errMsg = ObjectUtils.isEmpty(errorCode) ? e.getMessage() : DMErrorCode.getErrorMessage(errorCode);
                e.printStackTrace();
                throw new Exception(errMsg);
            }
            if (!DMErrorCode.OBJECT_ALREADY_EXISTS.equals(errorCode)) {
                // 撤销创建的用户
                sql = "DROP USER IF EXISTS \"" + username + "\"";
                try {
                    preparedStatement = conn.prepareStatement(sql);
                    preparedStatement.executeUpdate();
                    conn.commit();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            e.printStackTrace();
            throw new Exception(e.getMessage().contains("语法分析出错") ? "创建用户失败,权限组中存在不支持达梦数据库的权限命令" : e.getMessage());
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

    //postgresql 新增用户
    @Transactional(rollbackFor = ZAYKException.class)
    public int insertPostgreSqlUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws ZAYKException, SQLException {
        String sqlCreateUser, username, password, sql;
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        //Long permissionGroupId = dbhsmDbUser.getPermissionGroupId();
        username = dbhsmDbUser.getUserName();
        password = dbhsmDbUser.getPassword();
        boolean execute = false;
        //根据实例获取数据库连接
        connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
        if (!Optional.ofNullable(connection).isPresent()) {
            throw new ZAYKException("获取数据库连接失败！");
        }
        //创建postgresql用户sql
        try {
            sqlCreateUser = "CREATE USER \"" + username + "\" WITH PASSWORD '" + password + "'";
            preparedStatement = connection.prepareStatement(sqlCreateUser);
            execute = preparedStatement.execute();
        } catch (Exception e) {
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
            throw new ZAYKException(e.getMessage());
        }
        if (!execute) {
            int result = insertDbUsers(dbhsmDbUser);
            if (result != 1) {
                log.error("创建用户失败!");
                throw new ZAYKException("创建用户失败!");
            }
        }
        //根据权限组id查询权限组对应的所有权限SQL：
        //List<String> permissionsSqlList = dbhsmPermissionGroupMapper.getPermissionsSqlByPermissionsGroupid(permissionGroupId);
        //赋权
        //String permissionsSql = null;
        try {
            //for (String permission : permissionsSqlList) {
            //    permissionsSql = permission.toLowerCase();
            //    if (!(permissionsSql.startsWith("grant") && !(permissionsSql.startsWith("revoke")))) {
            //        log.info("不支持的授权SQL:" + permissionsSql);
            //        throw new ZAYKException("不支持的授权SQL:" + permissionsSql);
            //    }
            //    sql = permission.trim() + " ON SCHEMA " + dbhsmDbUser.getDbSchema() + " to \"" + username + "\"";
            //    preparedStatement = connection.prepareStatement(sql);
            //    preparedStatement.executeUpdate();
            //}
            sql = "GRANT ALL ON SCHEMA  \"" + dbhsmDbUser.getDbSchema() + "\" to \"" + username + "\"";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            //将schema下的所有表赋权给用户
            TablePermissionsGrantedToUsers(connection, dbhsmDbUser.getDbSchema(), username);
            //加解密函数
            ProcedureUtil.pgextFuncStringEncrypt(connection, dbhsmDbUser);
            ProcedureUtil.pgextFuncStringDecrypt(connection, dbhsmDbUser);
            //fpe函数
            ProcedureUtil.pgextFuncFPEEncrypt(connection, dbhsmDbUser);
            ProcedureUtil.pgextFuncFPEDecrypt(connection, dbhsmDbUser);
            connection.commit();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            try {
                // 回滚事务
                connection.rollback();
                //删除创建的用户
                sql = "drop OWNED BY \"" + username + "\";drop user \"" + username + "\"";
                log.info("删除数据库用户执行SQL:{}", sql);
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                throw new ZAYKException("授权失败！删除用户失败！");
            }
            throw new ZAYKException("授权失败!" + throwables.getMessage());
        } finally {
            //释放资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return 1;
    }

    /**
     * 将schema下的所有表赋权给用户
     */
    private void TablePermissionsGrantedToUsers(Connection connection, String dbSchema, String username) {
        PreparedStatement preparedStatement = null;
        String getAllTablesNameSql = "SELECT * FROM information_schema.tables WHERE table_schema = '" + dbSchema + "'";
        try {
            preparedStatement = connection.prepareStatement(getAllTablesNameSql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                String grantPermissionSql = "GRANT ALL ON TABLE \"" + dbSchema + "\".\"" + tableName + "\" TO \"" + username + "\"";
                preparedStatement = connection.prepareStatement(grantPermissionSql);
                boolean execute = preparedStatement.execute();
                log.info("grantPermissionSql:{},Sql执行返回：{}", grantPermissionSql, execute);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            //释放资源preparedStatement，connection不在此处释放，由调用者释放
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException sqlException) {
                    sqlException.printStackTrace();
                }
            }

        }
    }

    @Transactional(rollbackFor = ZAYKException.class)
    public int insertMysqlUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws ZAYKException, SQLException {
        String sqlCreateUser, username, password, sql;
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        Long permissionGroupId = dbhsmDbUser.getPermissionGroupId();
        int executeUpdate = 0;
        boolean execute = false;
        //根据实例获取数据库连接
        connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
        if (!Optional.ofNullable(connection).isPresent()) {
            throw new ZAYKException("数据库连接失败！");
        }
        username = dbhsmDbUser.getUserName();
        password = dbhsmDbUser.getPassword();

        try {
            //创建用户sql
            sqlCreateUser = "CREATE USER ?@'%' IDENTIFIED BY ?";
            preparedStatement = connection.prepareStatement(sqlCreateUser);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            execute = preparedStatement.execute();
        } catch (Exception e) {
            //释放资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains("Operation CREATE USER failed for")) {
                throw new ZAYKException(dbhsmDbUser.getUserName() + "用户已存在！");
            }
        }
        if (!execute) {
            int result = insertDbUsers(dbhsmDbUser);
            if (result != 1) {
                log.error("创建用户失败!");
                throw new ZAYKException("创建用户失败!");
            }
        }
        //根据权限组id查询权限组对应的所有权限SQL：
        List<String> permissionsSqlList = dbhsmPermissionGroupMapper.getPermissionsSqlByPermissionsGroupid(permissionGroupId);
        //赋权
        String permissionsSql = null;
        try {
            for (String permission : permissionsSqlList) {
                if (StringUtils.isNotEmpty(permission)) {
                    permissionsSql = permission.toLowerCase();
                    if (!(permissionsSql.startsWith("grant") && !(permissionsSql.startsWith("revoke")))) {
                        log.info("不支持的授权SQL:" + permissionsSql);
                        throw new ZAYKException("不支持的授权SQL:" + permissionsSql);
                    }
                    sql = permission.trim() + " on " + instance.getDatabaseServerName() + ".* to '" + username + "'@'%'";
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.executeUpdate();
                }
            }
            //sql = " GRANT ALL ON  " + instance.getDatabaseServerName() + ".* TO  " + username;
            //preparedStatement = connection.prepareStatement(sql);
            //preparedStatement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            // 回滚事务
            connection.rollback();
            // 撤销创建的用户
            sql = "DROP USER '" + username + "'@'%'";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            //释放资源
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                throwables.printStackTrace();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                throwables.printStackTrace();
            }
            throw new ZAYKException("创建用户失败：授权失败，MySQL不支持的授权SQL: " + permissionsSql);
        }
        sql = "FLUSH PRIVILEGES;";
        if (connection.isClosed()) {
            log.info("connection.isClosed(),重新获取连接");
            connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
        }
        preparedStatement = connection.prepareStatement(sql);
        executeUpdate = preparedStatement.executeUpdate();
        //创建加解密函数
        try {
            FunctionUtil.createEncryptDecryptFunction(connection, instance);
            connection.commit();
        } catch (Exception e) {
            log.info("创建加解密函数失败！加密吗函数已存在？" + e.getMessage());
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

    @Transactional(rollbackFor = Exception.class)
    public int insertSqlServerlUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws Exception {
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        //根据实例获取数据库连接
        connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
        if (!Optional.ofNullable(connection).isPresent()) {
            throw new ZAYKException("数据库连接获取失败");
        }
        String dbName = instance.getDatabaseServerName();
        String username = dbhsmDbUser.getUserName();
        String password = dbhsmDbUser.getPassword();
        try {
            //创建登录名sql
            String sqlCreateUser = "CREATE LOGIN [" + username + "] WITH PASSWORD=N'" + password + "', DEFAULT_DATABASE=[" + dbName + "], CHECK_EXPIRATION=OFF, CHECK_POLICY=OFF";
            preparedStatement = connection.prepareStatement(sqlCreateUser);
            boolean create = preparedStatement.execute();

            //将登录名映射成用户,默认架构放在dbo下
            sqlCreateUser = "CREATE USER [" + username + "] FOR LOGIN [" + username + "] WITH DEFAULT_SCHEMA=[dbo]";
            preparedStatement = connection.prepareStatement(sqlCreateUser);
            boolean use = preparedStatement.execute();

            //添加database的db_own权限，用于配置后续的加密对象。
            sqlCreateUser = "EXEC sp_addrolemember 'db_owner', '" + username + "'";
            preparedStatement = connection.prepareStatement(sqlCreateUser);
            boolean exec = preparedStatement.execute();
            log.info("创建登录用户：{}，将登录名放在dbo下：{}，添加db_own权限：{}", create, use, exec);

            connection.commit();
        } catch (Exception e) {
            //释放资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            throw new ZAYKException(e.getMessage().startsWith("服务器主体") ? "用户已存在" : e.getMessage());
        }
        int result = insertDbUsers(dbhsmDbUser);
        if (result != 1) {
            log.error("创建用户失败!");
            throw new ZAYKException("创建用户失败!");
        }

        DbhsmDbUser dbUser = new DbhsmDbUser();
        dbUser.setDatabaseType(DbConstants.DB_TYPE_SQLSERVER);
        List<DbhsmDbUser> dbhsmDbUsers = dbhsmDbUsersMapper.selectDbhsmDbUsersList(dbUser);
        if (dbhsmDbUsers.size() > 1) {
            return dbhsmDbUsers.size();
        }

        //创建sm4加解密方法 fpe方法
        FunctionUtil.createSqlServerFunction(connection);
        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public int insertOracleUser(DbhsmDbUser dbhsmDbUser, DbhsmDbInstance instance) throws Exception {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        int executeUpdate = 0;
        String username, password, tableSpace, sql, permissionsSql = null;
        dbhsmDbUser.setUserName(dbhsmDbUser.getUserName().toUpperCase());
        int result = insertDbUsers(dbhsmDbUser);
        if (result != 1) {
            log.error("创建用户失败!");
            throw new ZAYKException("创建用户失败!");
        }
        username = dbhsmDbUser.getUserName();
        password = dbhsmDbUser.getPassword();
        tableSpace = dbhsmDbUser.getTableSpace();
        Long permissionGroupId = dbhsmDbUser.getPermissionGroupId();
        //根据实例获取数据库连接
        conn = DbConnectionPoolFactory.getInstance().getConnection(instance);
        if (!Optional.ofNullable(conn).isPresent()) {
            throw new ZAYKException("获取数据库连接失败!");
        }
        try {
            sql = "CREATE USER ? IDENTIFIED BY ? DEFAULT TABLESPACE ? TEMPORARY TABLESPACE ?";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, "\"" + password + "\"");
            preparedStatement.setString(3, "\"" + tableSpace + "\"");
            preparedStatement.setString(4, "TEMP");
            preparedStatement.executeUpdate();
        } catch (Exception e) {
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
            throw new Exception(e);
        }
        //根据权限组id查询权限组对应的所有权限SQL：
        List<String> permissionsSqlList = dbhsmPermissionGroupMapper.getPermissionsSqlByPermissionsGroupid(permissionGroupId);
        //赋权
        try {
            for (int i = 0; i < permissionsSqlList.size(); i++) {
                permissionsSql = permissionsSqlList.get(i);
                if (StringUtils.isNotEmpty(permissionsSql)) {
                    if (!(permissionsSql.toLowerCase().startsWith("grant") && !(permissionsSql.toLowerCase().startsWith("revoke")))) {
                        log.info("不支持的授权SQL:" + permissionsSql);
                        throw new ZAYKException("不支持的授权SQL:" + permissionsSql);
                    }
                    if (permissionsSql.toLowerCase().startsWith("grant")) {
                        sql = permissionsSql.trim() + " to ?";
                    } else {
                        sql = permissionsSql.trim() + " from ?";
                    }
                    preparedStatement = conn.prepareStatement(sql);
                    preparedStatement.setString(1, username);
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException sqlException) {
            // 回滚事务
            conn.rollback();
            // 撤销创建的用户
            sql = "DROP USER ? cascade";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.executeUpdate();
            sqlException.printStackTrace();
            //释放资源
            try {
                preparedStatement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            try {
                conn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            throw new ZAYKException("创建用户失败：授权失败，Oracle不支持的授权SQL:" + permissionsSql);
        }
        try {
            conn.commit();
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
            conn.commit();

            //赋执行库文件liboraextapi的权限
            sql = "CREATE OR REPLACE LIBRARY liboraextapi AS ?";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, dbhsmDbUser.getEncLibapiPath());
            preparedStatement.execute();

            sql = "grant execute on liboraextapi to ?";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.execute();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            // 回滚事务
            conn.rollback();
            // 撤销创建的用户
            sql = "DROP USER ? cascade";
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.executeUpdate();
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
        int i = dbhsmDbUsersMapper.insertDbhsmDbUsers(dbhsmDbUser);
        //新增用户与权限组对应关系
        if (dbhsmDbUser.getDatabaseType().equals(DbConstants.DB_TYPE_ORACLE)) {
            DbhsmUserPermissionGroup permissionGroup = new DbhsmUserPermissionGroup();
            permissionGroup.setUserId(dbhsmDbUser.getId());
            permissionGroup.setPermissionGroupId(dbhsmDbUser.getPermissionGroupId());
            dbhsmUserPermissionGroupMapper.insertDbhsmUserPermissionGroup(permissionGroup);
        }
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
    @Transactional(rollbackFor = {ZAYKException.class, SQLException.class})
    public int deleteDbhsmDbUsersByIds(Long[] ids) throws SQLException, ZAYKException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        int resultSet = 0;
        String sql = "";
        List<String> errorList = new ArrayList<>();
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
                    switch (instance.getDatabaseType()) {
                        case DbConstants.DB_TYPE_ORACLE:
                            sql += " cascade";
                            //销毁用户连接池
                            //destroyUserConnPool(instance,userName);
                            break;
                        case DbConstants.DB_TYPE_SQLSERVER:
                            sql += ";drop login " + userName;
                            break;
                        case DbConstants.DB_TYPE_MYSQL:
                            sql = "drop user '" + userName + "'@'%'";
                            break;
                        case DbConstants.DB_TYPE_POSTGRESQL:
                            sql = "drop OWNED BY \"" + userName + "\";drop user \"" + userName + "\"";
                            break;
                        case DbConstants.DB_TYPE_DM:
                            sql = "DROP USER IF EXISTS \"" + userName + "\" cascade";
                            break;
                        default:
                            throw new ZAYKException("暂不支持的数据库类型： " + instance.getDatabaseType());
                    }
                    log.info("删除数据库用户执行SQL:{}", sql);
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.execute();
                    connection.commit();
                }
            } catch (SQLException e) {
                log.info("删除数据库用户失败!执行SQL:{}", sql);
                e.printStackTrace();
                errorList.add(userName);
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
        if (errorList.size() > 0) {
            throw new ZAYKException("删除以下数据库用户失败!失败用户列表:" + StringUtils.join(errorList, ","));
        }
        return resultSet;
    }

    private void checkEncColConf(String userName, Long instanceId) throws ZAYKException {
        DbhsmEncryptColumns encryptColumns = new DbhsmEncryptColumns();
        encryptColumns.setDbUserName(userName);
        encryptColumns.setDbInstanceId(instanceId);
        List<DbhsmEncryptColumns> encryptColumnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(encryptColumns);
        if (!CollectionUtils.isEmpty(encryptColumnsList)) {
            String dbUserNameStr = encryptColumnsList.stream().map(DbhsmEncryptColumns::getDbTable).distinct().collect(Collectors.joining(","));
            throw new ZAYKException(userName + "用户下的表:" + dbUserNameStr + "已配置加密列，不允许删除！");
        }
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
        SysDictData sysDictData;
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

                List<DbhsmDbInstance> instanceList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(new DbhsmDbInstance());
                for (int j = 0; j < instanceList.size(); j++) {
                    DbhsmDbInstance instance = instanceList.get(j);
                    if (sysDictData.getDictValue().equals(instance.getDatabaseType())) {
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

    @Override
    public String dmPwdPolicyValidate(DbhsmDbUser dbhsmDbUser) {
        int pwdPolicyToDM = dbhsmDbInstanceService.getPwdPolicyToDM(dbhsmDbUser.getDatabaseInstanceId());
        int pwdMinLenToDM = dbhsmDbInstanceService.getPwdMinLenToDM(dbhsmDbUser.getDatabaseInstanceId());
        return DMUserPasswordPolicy.validatePolicyRules(pwdPolicyToDM, dbhsmDbUser.getPassword(), dbhsmDbUser.getUserName(), pwdMinLenToDM);
    }


}
