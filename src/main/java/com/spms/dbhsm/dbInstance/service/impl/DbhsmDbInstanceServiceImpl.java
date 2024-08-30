package com.spms.dbhsm.dbInstance.service.impl;

import cn.hutool.core.util.RuntimeUtil;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.bean.BeanConvertUtils;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.common.SelectOption;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.FunctionUtil;
import com.spms.common.pool.hikariPool.DbConnectionPool;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.domain.VO.InstanceServerNameVO;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbInstance.service.IDbhsmDbInstanceService;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 数据库实例Service业务层处理
 *
 * @author spms
 * @date 2023-09-19
 */
@Service
@Slf4j
public class DbhsmDbInstanceServiceImpl implements IDbhsmDbInstanceService {
    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmDbUsersMapper dbhsmDbUsersMapper;

    @Autowired
    private DbhsmEncryptColumnsMapper encryptColumnsMapper;

    @PostConstruct
    public void init() {
        // 创建线程1
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                initDbConnectionPool();
            }
        });
        executor.shutdown();
    }

    public void initDbConnectionPool() {
        try {
            List<DbhsmDbInstance> dbhsmDbInstanceList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(null);
            if (CollectionUtils.isEmpty(dbhsmDbInstanceList)) {
                log.info("数据库实例列表为空，无需初始化数据库连接池");
                return;
            }
            for (DbhsmDbInstance dbhsmDbInstance : dbhsmDbInstanceList) {
                if (DbConstants.DB_TYPE_HB.equals(dbhsmDbInstance.getDatabaseType())) {
                    continue;
                }
                DbInstancePoolKeyDTO instanceKey = new DbInstancePoolKeyDTO();
                BeanUtils.copyProperties(dbhsmDbInstance, instanceKey);
                DbInstanceGetConnDTO instanceGetConnDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(dbhsmDbInstance, instanceGetConnDTO);
                try {
                    DbConnectionPoolFactory.buildDataSourcePool(instanceGetConnDTO);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("初始化数据库连接池失败:{}", e.getMessage());
                }
            }
            DbConnectionPoolFactory.queryPool();
            log.info("初始化数据库连接池成功");
        } catch (Exception e) {

        }
    }

    /**
     * 查询数据库实例
     *
     * @param id 数据库实例主键
     * @return 数据库实例
     */
    @Override
    public DbhsmDbInstance selectDbhsmDbInstanceById(Long id) {
        return dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
    }

    @Override
    public List<InstanceServerNameVO> listDbInstanceSelect(InstanceServerNameVO instanceServerNameVO) {
        List<InstanceServerNameVO> voList = dbhsmDbInstanceMapper.listDbInstanceSelect(instanceServerNameVO);
        for (InstanceServerNameVO vo : voList) {
            switch (vo.getDatabaseType()) {
                case DbConstants.DB_TYPE_ORACLE:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_ORACLE_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_SQLSERVER:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_SQLSERVER_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_MYSQL:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_MYSQL_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_POSTGRESQL:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_POSTGRESQL_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_DM:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_DM_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_CLICKHOUSE:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_CLICKHOUSE_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_KB:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_KING_BASE_DESC + ")");
                    break;
                default:
                    // 处理未知的数据库类型
                    break;
            }
        }
        return voList;
    }

    /**
     * 查询数据库实例列表
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 数据库实例
     */
    @Override
    public List<DbhsmDbInstance> selectDbhsmDbInstanceList(DbhsmDbInstance dbhsmDbInstance) {
        return dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(dbhsmDbInstance);
    }

    /**
     * 新增数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException, SQLException {
        paramCheck(dbhsmDbInstance);

        dbhsmDbInstance.setCreateTime(com.zayk.util.DateUtils.getNowDate());
        int i = dbhsmDbInstanceMapper.insertDbhsmDbInstance(dbhsmDbInstance);
        //创建连接池
        DbInstanceGetConnDTO dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, dbInstanceGetConnDTO);
        DbConnectionPoolFactory.buildDataSourcePool(dbInstanceGetConnDTO);
        DbConnectionPoolFactory.queryPool();

        //代理端口
        Integer proxyPort = dbhsmDbInstance.getProxyPort();

        String patten = "1400[0-9]|140[12][0-9]|14030|14031";
        boolean matches = Pattern.matches(patten, proxyPort.toString());
        if (!matches) {
            throw new ZAYKException("代理端口范围为：14000-14031");
        }

        boolean b = addProxyPort(proxyPort, proxyPort);
        if (!b) {
            throw new ZAYKException("该端口已被使用：" + proxyPort);
        }

        return i;
    }

    public static boolean addProxyPort(int port, int newProxyPort) {
        String firewallStatus = RuntimeUtil.execForStr("firewall-cmd --state");
        if (firewallStatus.contains("not running")) {
            //防火墙未开启，跳过添加端口步骤
            log.error("防火墙未开启.............");
            return true;
        }
        String execForStr = RuntimeUtil.execForStr("firewall-cmd --query-port=" + newProxyPort + "/tcp");
        if (port != newProxyPort) {
            if (execForStr.contains("no")) {
                //开通端口号
                RuntimeUtil.execForStr("firewall-cmd --permanent --remove-port=" + port + "/tcp");
                RuntimeUtil.execForStr("firewall-cmd --zone=public --add-port=" + newProxyPort + "/tcp --permanent");
                RuntimeUtil.execForStr("firewall-cmd --reload");
                log.info("Nginx Firewall Port Add Successful! ");
                return true;
            }
        } else {
            if (execForStr.contains("no")) {
                RuntimeUtil.execForStr("firewall-cmd --zone=public --add-port=" + newProxyPort + "/tcp --permanent");
                RuntimeUtil.execForStr("firewall-cmd --reload");
                log.info("ServerPortUtil.openPortOfUpdate()=============> oldPort：[" + port + "], latestPort：[" + newProxyPort + "]");
                return true;
            }
        }
        return false;
    }

    public static void closePortOfDelete(List<String> ports) {
        // 对比端口是否开放
        if (!CollectionUtils.isEmpty(ports)) {
            String firewallStatus = RuntimeUtil.execForStr("firewall-cmd --state");
            if (!firewallStatus.contains("not running")) {
                ports.stream().distinct().forEach((port) -> RuntimeUtil.execForStr("firewall-cmd --permanent --remove-port=" + port + "/tcp"));
                RuntimeUtil.execForStr("firewall-cmd --reload");
                log.info("Nginx Firewall Port Close Successful! ");
            } else {
                log.info("ServerPortUtil.closePortOfDelete()=============> 防火墙未启动");
            }
        }
    }

    public void paramCheck(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException {
        if (DbConstants.DB_TYPE_MYSQL.equals(dbhsmDbInstance.getDatabaseType())) {
            if (dbhsmDbInstance.getDatabaseDba().length() > 32 || dbhsmDbInstance.getServiceUser().length() > 32) {
                throw new ZAYKException("数据库类型为：mysql，用户名最长为32字符");
            }
            if (dbhsmDbInstance.getDatabaseDbaPassword().length() > 256 || dbhsmDbInstance.getServicePassword().length() > 256) {
                throw new ZAYKException("数据库类型为：mysql，密码最长为256字符");
            }
            if (dbhsmDbInstance.getDatabaseServerName().length() > 64) {
                throw new ZAYKException("数据库类型为：mysql，数据库名称最长为64字符");
            }
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equals(dbhsmDbInstance.getDatabaseType())) {
            if (dbhsmDbInstance.getDatabaseDba().length() > 64 || dbhsmDbInstance.getServiceUser().length() > 64) {
                throw new ZAYKException("数据库类型为：PostgreSQL，用户名最长为64字符");
            }
            if (dbhsmDbInstance.getDatabaseDbaPassword().length() > 128 || dbhsmDbInstance.getServicePassword().length() > 128) {
                throw new ZAYKException("数据库类型为：PostgreSQL，密码最长为256字符");
            }
            if (dbhsmDbInstance.getDatabaseServerName().length() > 64) {
                throw new ZAYKException("数据库类型为：PostgreSQL，数据库名称最长为64字符");
            }
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())) {
            if (dbhsmDbInstance.getDatabaseDba().length() > 128 || dbhsmDbInstance.getServiceUser().length() > 128) {
                throw new ZAYKException("SQL Server用户名最长128字符");
            }
            if (dbhsmDbInstance.getDatabaseDbaPassword().length() > 128 || dbhsmDbInstance.getServicePassword().length() > 128) {
                throw new ZAYKException("SQL Server密码最长为128字符");
            }
            if (dbhsmDbInstance.getDatabaseServerName().length() > 128) {
                throw new ZAYKException("数据库类型为：SQL Server，数据库名称最长为128字符");
            }
        }
    }

    /**
     * 修改数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException, SQLException {
        paramCheck(dbhsmDbInstance);
        if (DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(editCheckDBOracleInstanceUnique(dbhsmDbInstance))) {
                throw new ZAYKException("修改失败，数据库实例" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() + dbhsmDbInstance.getDatabaseServerName() + "已存在");
            }
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())) {
            //数据库实例唯一性判断
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(editCheckDBSqlServerUnique(dbhsmDbInstance))) {
                throw new ZAYKException("修改失败，数据库实例" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() + dbhsmDbInstance.getDatabaseServerName() + "已存在");
            }
        }
        DbhsmDbInstance instanceById = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbhsmDbInstance.getId());
        if (null != instanceById.getProxyPort() && !instanceById.getProxyPort().equals(dbhsmDbInstance.getProxyPort())) {
            //修改端口
            boolean b = addProxyPort(instanceById.getProxyPort(), dbhsmDbInstance.getProxyPort());
            if (!b) {
                throw new ZAYKException("该端口已被使用：" + instanceById.getProxyPort());
            }
        }

        dbhsmDbInstance.setUpdateTime(DateUtils.getNowDate());
        int i = dbhsmDbInstanceMapper.updateDbhsmDbInstance(dbhsmDbInstance);
        //如果创建连接池需要的数据数据未做修改，不需要重新建链接，否则需要在修改时先销毁之前的池，再生成新连接池
        if (DbInstanceGetConnDTO.instanceConvertGetConnDTO(instanceById).equals(DbInstanceGetConnDTO.instanceConvertGetConnDTO(dbhsmDbInstance))) {
            return i;
        }
        //删除动态数据连接池中名称为dbhsmDbInstance的连接池
        DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(dbhsmDbInstance));
        //重建连接池
        DbInstanceGetConnDTO dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, dbInstanceGetConnDTO);
        DbConnectionPoolFactory.buildDataSourcePool(dbInstanceGetConnDTO);
        DbConnectionPoolFactory.queryPool();
        return i;
    }


    public String editCheckDBOracleInstanceUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        instance.setDatabaseServerName(dbhsmDbInstance.getDatabaseServerName());
        instance.setDatabaseExampleType(dbhsmDbInstance.getDatabaseExampleType());
        List<DbhsmDbInstance> infoList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (!CollectionUtils.isEmpty(infoList)) {
            if (!dbhsmDbInstance.getId().equals(infoList.get(0).getId())) {
                return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
            }
        }
        return DbConstants.DBHSM_GLOBLE_UNIQUE;
    }

    public String editCheckDBSqlServerUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        List<DbhsmDbInstance> sqlServerList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (sqlServerList.size() > 0) {
            if (!dbhsmDbInstance.getId().equals(sqlServerList.get(0).getId())) {
                return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
            }
        }
        return DbConstants.DBHSM_GLOBLE_UNIQUE;
    }

    /**
     * 批量删除数据库实例
     *
     * @param ids 需要删除的数据库实例主键
     * @return 结果
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteDbhsmDbInstanceByIds(Long[] ids) {
        int i = 0;
        List<String> isUsedInstances = new ArrayList<String>();
        List<String> ports = new ArrayList<>();
        for (Long id : ids) {
            //删除之前先销毁之前的池
            DbhsmDbInstance instanceById = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
            //查看实例是否创建过用户
            if (checkInstanceCreatedUser(id)) {
                isUsedInstances.add(getInstance(instanceById));
                continue;
            }
            //查询该实例下是否有加密列
            List<DbhsmEncryptColumns> dbhsmEncryptColumns = encryptColumnsMapper.queryEncryptColumnsByInstanceId(id);
            if (!CollectionUtils.isEmpty(dbhsmEncryptColumns)) {
                return AjaxResult.error("数据库资产为：" + instanceById.getDatabaseCapitalName() + "，存在加密列配置信息，无法进行删除操作");
            }

            i = dbhsmDbInstanceMapper.deleteDbhsmDbInstanceById(id);
            try {
                //添加端口信息，统一删除
                ports.add(instanceById.getProxyPort().toString());
                //删除连接池
                DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(instanceById));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("删除数据库失败失败：{}", e.getMessage());
            }
        }
        //删除端口
        log.info("需要删掉的端口信息：{}", ports.stream().toString());
        closePortOfDelete(ports);
        return !CollectionUtils.isEmpty(isUsedInstances) ? AjaxResult.error("实例：" + StringUtils.join(isUsedInstances, ",") + "已从管理端创建过用户，无法删除！") : AjaxResult.success();
    }

    private void delEncDecFunction(DbhsmDbInstance instanceById) {
        String delEncFunSql = "", delDecFunSql = "";
        try {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(instanceById);
            String databaseType = instanceById.getDatabaseType();
            //删除mysql加解密函数
            switch (databaseType) {
                case DbConstants.DB_TYPE_ORACLE:
                    break;
                case DbConstants.DB_TYPE_SQLSERVER:
                    break;
                case DbConstants.DB_TYPE_MYSQL:
                    delEncFunSql = "DROP FUNCTION StringEncrypt;";
                    delDecFunSql = "DROP FUNCTION StringDecrypt;";
                    break;
                case DbConstants.DB_TYPE_POSTGRESQL:
                    break;
                case DbConstants.DB_TYPE_DM:
                    break;
                default:
                    log.info("Unknown database type: " + databaseType);
            }
            //执行SQL
            if (!StringUtils.isEmpty(delEncFunSql)) {
                connection.prepareStatement(delEncFunSql).executeUpdate();
                connection.prepareStatement(delDecFunSql).executeUpdate();
                connection.commit();
            }
        } catch (ZAYKException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除数据库实例信息
     *
     * @param id 数据库实例主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbInstanceById(Long id) {
        return dbhsmDbInstanceMapper.deleteDbhsmDbInstanceById(id);
    }

    /**
     * 查询Oracle表空间
     */
    @Override
    public List<SelectOption> getDbTablespace(Long id) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        int i = 0;
        List<SelectOption> tablespaceList = new ArrayList<>();
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType()) || DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
            if (!ObjectUtils.isEmpty(instance)) {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                try {
                    conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                    if (Optional.ofNullable(conn).isPresent()) {
                        stmt = conn.createStatement();
                        String selectTableSpaceSql = null;
                        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType())) {
                            selectTableSpaceSql = DbConstants.DB_SQL_ORACLE_TABLESPACE_QUERY;
                        } else if (DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
                            selectTableSpaceSql = DbConstants.DB_SQL_DM_TABLESPACE_QUERY;
                        }
                        resultSet = stmt.executeQuery(selectTableSpaceSql);
                        while (resultSet.next()) {
                            SelectOption option = new SelectOption();
                            option.setId(i++);
                            option.setLabel(resultSet.getString("tablespace_name"));
                            tablespaceList.add(option);
                        }
                    }
                } catch (SQLException | ZAYKException e) {
                    e.printStackTrace();
                } finally {
                    if (stmt != null) {
                        try {
                            stmt.close();
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
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            return Collections.emptyList();
        }
        return tablespaceList;
    }

    //获取PostgreSQL 架构（schema)
    @Override
    public List<SelectOption> getDbSchema(Long id) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        int i = 0;
        String sql = null;
        List<SelectOption> list = new ArrayList<>();
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (DbConstants.DB_TYPE_POSTGRESQL.equals(instance.getDatabaseType())) {
            if (!ObjectUtils.isEmpty(instance)) {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                try {
                    conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                    if (Optional.ofNullable(conn).isPresent()) {
                        stmt = conn.createStatement();
                        sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('pg_catalog', 'pg_toast', 'information_schema');";
                        resultSet = stmt.executeQuery(sql);
                        while (resultSet.next()) {
                            SelectOption option = new SelectOption();
                            option.setId(i++);
                            option.setLabel(resultSet.getString("schema_name"));
                            list.add(option);
                        }
                    }
                } catch (SQLException | ZAYKException e) {
                    e.printStackTrace();
                } finally {
                    if (stmt != null) {
                        try {
                            stmt.close();
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
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            return Collections.emptyList();
        }
        return list;
    }

    public boolean checkInstanceCreatedUser(Long instanceId) {
        DbhsmDbUser user = new DbhsmDbUser();
        user.setDatabaseInstanceId(instanceId);
        List<DbhsmDbUser> dbhsmDbUsers = dbhsmDbUsersMapper.selectDbhsmDbUsersList(user);
        return dbhsmDbUsers.size() > 0;
    }

    private String getInstance(DbhsmDbInstance instance) {
        String databaseType = "";
        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_ORACLE_DESC;
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_SQLSERVER_DESC;
        } else if (DbConstants.DB_TYPE_MYSQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_MYSQL_DESC;
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_POSTGRESQL_DESC;
        } else if (DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_DM_DESC;
        }
        return databaseType + ":" + instance.getDatabaseIp() + ":" + instance.getDatabasePort() + instance.getDatabaseExampleType() + instance.getDatabaseServerName();
    }

    @Override
    public int getPwdPolicyToDM(Long id) {
        int pwdPolicyToDM = 2;
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        try {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            pwdPolicyToDM = FunctionUtil.getPwdPolicyToDM(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pwdPolicyToDM;
    }

    @Override
    public int getPwdMinLenToDM(Long id) {
        int pwdMinLenToDM = 9;
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        try {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            pwdMinLenToDM = FunctionUtil.getPwdMinLenToDM(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pwdMinLenToDM;
    }


    @Override
    public AjaxResult2<Boolean> connectionTest(DbhsmDbInstance instance) {
        DbInstanceGetConnDTO dto = BeanConvertUtils.beanToBean(instance, DbInstanceGetConnDTO.class);
        if (null != instance.getId()) {
            DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(instance.getId());
            if (null == dbhsmDbInstance) {
                return AjaxResult2.success(false);
            }
            dto = BeanConvertUtils.beanToBean(dbhsmDbInstance, DbInstanceGetConnDTO.class);
        }

        // 创建数据库连接
        try {
            javax.sql.DataSource dataSourcePool = DbConnectionPool.initialize(dto);
            Connection connection = dataSourcePool.getConnection();
            if (null != connection && !connection.isClosed()) {
                log.info("数据库测试连接成功！");
                // 关闭连接
                connection.close();
            }
        } catch (SQLException e) {
            log.error("数据库连接失败：{}", e.getMessage());
            return AjaxResult2.success(false);
        }
        return AjaxResult2.success(true);
    }

    @Override
    public AjaxResult2<Boolean> openProxy(Long id) {
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (null == dbhsmDbInstance) {
            return AjaxResult2.success("实例信息错误！", false);
        }
        //执行脚本


        return AjaxResult2.success(true);
    }

    @Override
    public AjaxResult2<Boolean> proxyTest(Long id) {
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (null == dbhsmDbInstance) {
            return AjaxResult2.success("测试连接失败，实例信息错误！", false);
        }
        return AjaxResult2.success(true);
    }
}
