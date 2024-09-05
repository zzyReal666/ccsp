package com.spms.dbhsm.dbInstance.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.bean.BeanConvertUtils;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.common.SelectOption;
import com.spms.common.Template.FreeMarkerTemplateEngine;
import com.spms.common.Template.TemplateEngine;
import com.spms.common.Template.TemplateEngineException;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.FunctionUtil;
import com.spms.common.pool.hikariPool.DbConnectionPool;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.common.shell.ShellScriptExecutor;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    /**
     * zk地址
     */
    @Value("${encrypt.zookeeper.url:localhost:2181}")
    private String zkAddress;

    /**
     * shell 的存放路径
     */
    @Value("${encrypt.shell.path:/opt/spms/spms/spms-dbenc-manager/}")
    private String shellPath;


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
        boolean b1 = checkCapitalName(dbhsmDbInstance.getDatabaseCapitalName());
        if (!b1) {
            throw new ZAYKException("资产名称为：" + dbhsmDbInstance.getDatabaseCapitalName() + ",已被占用");
        }

        dbhsmDbInstance.setCreateTime(com.zayk.util.DateUtils.getNowDate());
        int i = dbhsmDbInstanceMapper.insertDbhsmDbInstance(dbhsmDbInstance);
        //创建连接池
        DbInstanceGetConnDTO dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, dbInstanceGetConnDTO);
        DbConnectionPoolFactory.buildDataSourcePool(dbInstanceGetConnDTO);
        DbConnectionPoolFactory.queryPool();

        if (DbConstants.DL_PLUG.equals(dbhsmDbInstance.getPlugMode())) {
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
        }

        if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType()) && DbConstants.BE_PLUG.equals(dbhsmDbInstance.getPlugMode()) ) {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(dbhsmDbInstance);
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement("ALTER DATABASE [" + dbhsmDbInstance.getDatabaseServerName() + "] SET TRUSTWORTHY ON");
                boolean alter = statement.execute();
                statement = connection.prepareStatement("EXEC sp_configure 'clr enabled', 1  ");
                boolean exec = statement.execute();
                statement = connection.prepareStatement("CREATE ASSEMBLY libsqlextdll FROM '" + dbhsmDbInstance.getEncLibapiPath() + "'  WITH permission_set = UnSafe;");
                boolean use = statement.execute();
                log.info("开启数据库TRUSTWORTHY：{}，开启CLR：{}，创建程序集：{}", alter, exec, use);
                connection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                statement.close();
                connection.close();
            }
        }


        return i;
    }

    public boolean checkCapitalName(String name) {
        List<DbhsmDbInstance> dbhsmDbInstances = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(new DbhsmDbInstance());
        if (CollectionUtils.isEmpty(dbhsmDbInstances)) {
            return true;
        }
        List<String> collect = dbhsmDbInstances.stream().map(DbhsmDbInstance::getDatabaseCapitalName).collect(Collectors.toList());
        return !collect.contains(name);
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

    public static void closePortOfDelete(String ports) {
        // 对比端口是否开放
        if (StringUtils.isNotBlank(ports)) {
            String firewallStatus = RuntimeUtil.execForStr("firewall-cmd --state");
            if (!firewallStatus.contains("not running")) {
                RuntimeUtil.execForStr("firewall-cmd --permanent --remove-port=" + ports + "/tcp");
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
        if (!instanceById.getDatabaseCapitalName().equals(dbhsmDbInstance.getDatabaseCapitalName())) {
            boolean b1 = checkCapitalName(dbhsmDbInstance.getDatabaseCapitalName());
            if (!b1) {
                throw new ZAYKException("资产名称为：" + dbhsmDbInstance.getDatabaseCapitalName() + ",已被占用");
            }
        }
        if (DbConstants.DL_PLUG.equals(instanceById.getPlugMode()) && null != instanceById.getProxyPort() && !instanceById.getProxyPort().equals(dbhsmDbInstance.getProxyPort())) {
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
    public AjaxResult2 deleteDbhsmDbInstanceByIds(Long[] ids) {
        int i = 0;
        List<String> isUsedInstances = new ArrayList<String>();
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
                return AjaxResult2.error("数据库资产为：" + instanceById.getDatabaseCapitalName() + "，存在加密列配置信息，无法进行删除操作");
            }

            i = dbhsmDbInstanceMapper.deleteDbhsmDbInstanceById(id);
            try {
                //添加端口信息，统一删除
                if (DbConstants.DL_PLUG.equals(instanceById.getPlugMode())) {
                    log.info("需要删掉的端口信息：{}", instanceById.getProxyPort().toString());
                    closePortOfDelete(instanceById.getProxyPort().toString());
                }
                //删除连接池
                DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(instanceById));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("删除数据库失败失败：{}", e.getMessage());
            }
        }
        return !CollectionUtils.isEmpty(isUsedInstances) ? AjaxResult2.error("实例：" + StringUtils.join(isUsedInstances, ",") + "已从管理端创建过用户，无法删除！") : AjaxResult2.success();
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


    private static final Map<Integer, String> CODE_MESSAGE = new HashMap<>();

    static {
        CODE_MESSAGE.put(0, "成功");
        CODE_MESSAGE.put(1, "缺少参数：实例ID！");
        CODE_MESSAGE.put(2, "创建目录失败！");
        CODE_MESSAGE.put(3, "复制ext-lib文件失败");
        CODE_MESSAGE.put(4, "docker 开启失败！");
    }

    @Override
    public AjaxResult2<Boolean> openProxy(Long id) {
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (null == dbhsmDbInstance) {
            return AjaxResult2.error("实例信息错误！");
        }
        if (!"2".equals(dbhsmDbInstance.getDatabaseType()) && !"3".equals(dbhsmDbInstance.getDatabaseType())) {
            return AjaxResult2.error("不支持的数据库类型！");
        }
        AjaxResult2<Boolean> error;
        //创建 conf 目录，复制ext-lib下的文件
        error = mkdir(id);
        if (error != null) {
            return error;
        }
        //上传配置文件
        error = uploadConfigFile(dbhsmDbInstance);
        if (error != null) {
            return error;
        }
        //启动docker
        return startUpDocker(id, dbhsmDbInstance.getProxyPort());
    }

    private AjaxResult2<Boolean> startUpDocker(Long id, Integer proxyPort) {
        ShellScriptExecutor.ExecutionResult executionResult = ShellScriptExecutor.executeScript(shellPath + "docker.sh", 60, "start", String.valueOf(id), String.valueOf(proxyPort));
        if (executionResult.getExitCode() != 0) {
            String errorMessage = CODE_MESSAGE.getOrDefault(executionResult.getExitCode(), "未知错误！");
            log.error("执行manage_docker_container.sh失败，错误码:{},错误信息：{},echo内容:{}", executionResult.getExitCode(), errorMessage, executionResult.getOutput());
            return AjaxResult2.error(errorMessage);
        } else {
            log.info("执行manage_docker_container.sh成功，echo内容:{}", executionResult.getOutput());
            return AjaxResult2.success("开启代理成功", true);
        }
    }

    private AjaxResult2<Boolean> uploadConfigFile(DbhsmDbInstance dbhsmDbInstance) {
        //服务器下载文件到conf目录
        try {
            //写入/opt/db_enc/docker_v/proxy_${db_id}/conf/global.yaml
            String globalConfigPath = "/opt/db_enc/docker_v/proxy_" + dbhsmDbInstance.getId() + "/conf/global.yaml";
            String globalConfig = generateGlobalConfigFile(dbhsmDbInstance);
            log.info("globalConfigPath:{},globalConfig:{}", globalConfigPath, globalConfig);
            FileUtil.writeUtf8String(globalConfig, globalConfigPath);

            //写入/opt/db_enc/docker_v/proxy_${db_id}/conf/database-encrypt.yaml
            String encryptConfigPath = "/opt/db_enc/docker_v/proxy_" + dbhsmDbInstance.getId() + "/conf/database-encrypt.yaml";
            String encryptConfig = generateEncryptConfigFile(dbhsmDbInstance);
            log.info("encryptConfigPath:{},encryptConfig:{}", encryptConfigPath, encryptConfig);
            FileUtil.writeUtf8String(encryptConfig, encryptConfigPath);
        } catch (Exception e) {
            return AjaxResult2.success("配置文件生成失败：", false);
        }
        return null;
    }

    public String generateEncryptConfigFile(DbhsmDbInstance dbhsmDbInstance) throws TemplateEngineException {
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();
        templateEngine.setTemplateFromFile("database-encrypt.ftl");
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("databaseName", dbhsmDbInstance.getDatabaseServerName());
        dataModel.put("username", dbhsmDbInstance.getServiceUser());
        dataModel.put("password", dbhsmDbInstance.getServicePassword());
        //mysql "*.*"  pg ds.*
        if ("2".equals(dbhsmDbInstance.getDatabaseType())) {
            dataModel.put("url", "jdbc:mysql://" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() + "/" + dbhsmDbInstance.getDatabaseServerName());
            dataModel.put("singleTable", "\"*.*\"");
        } else if ("3".equals(dbhsmDbInstance.getDatabaseType())) {
            dataModel.put("url", "jdbc:postgresql://" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() + "/" + dbhsmDbInstance.getDatabaseServerName());
            dataModel.put("singleTable", "\"*.*.*\"");
        }
        templateEngine.setDataModel(dataModel);
        return templateEngine.process();
    }

    public String generateGlobalConfigFile(DbhsmDbInstance dbhsmDbInstance) throws TemplateEngineException {
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();
        templateEngine.setTemplateFromFile("global.ftl");
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("ip", dbhsmDbInstance.getDatabaseIp());
        dataModel.put("port", dbhsmDbInstance.getDatabasePort());
        dataModel.put("username", dbhsmDbInstance.getServiceUser());
        dataModel.put("password", dbhsmDbInstance.getServicePassword());
        dataModel.put("zookeeperIp", zkAddress);
        templateEngine.setDataModel(dataModel);
        return templateEngine.process();
    }

    private AjaxResult2<Boolean> mkdir(Long id) {
//        URL mkdir = getClass().getClassLoader().getResource("shell/mkdir.sh");
//        if (null == mkdir) {
//            return AjaxResult2.error("mkdir.sh脚本文件不存在！");
//        }
        ShellScriptExecutor.ExecutionResult mkdirResult = ShellScriptExecutor.executeScript(shellPath + "mkdir.sh", 30, String.valueOf(id));
        if (mkdirResult.getExitCode() != 0) {
            String errorMessage = CODE_MESSAGE.getOrDefault(mkdirResult.getExitCode(), "未知错误！");
            log.error("执行mkdir.sh失败，错误码:{},错误信息：{},echo内容:{}", mkdirResult.getExitCode(), errorMessage, mkdirResult.getOutput());
            return AjaxResult2.success(errorMessage, false);
        }
        log.info("执行mkdir.sh成功，echo内容:{}", mkdirResult.getOutput());
        return null;
    }

    @Override
    public AjaxResult2<Boolean> proxyTest(Long id) throws ZAYKException, SQLException {
        DbhsmDbInstance db = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        //JDBC连接测试
        int timeout = 10;
        Connection connection = DbConnectionPoolFactory.getInstance().getConnection(db);
        if (connection != null && connection.isValid(timeout)) {
            log.info("数据库连接测试成功！");
            return AjaxResult2.success("数据库连接测试成功！", true);
        } else {
            log.error("数据库连接测试失败！");
            return AjaxResult2.success("数据库连接测试失败！", false);
        }
    }
}
