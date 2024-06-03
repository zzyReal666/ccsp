package com.spms.common.task;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.SM3Util;
import com.spms.common.CommandUtil;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.warningConfig.domain.DbhsmWarningConfig;
import com.spms.dbhsm.warningConfig.mapper.DbhsmWarningConfigMapper;
import com.spms.dbhsm.warningConfig.vo.DbhsmWarningConfigListResponse;
import com.spms.dbhsm.warningFile.domain.DbhsmIntegrityFileConfig;
import com.spms.dbhsm.warningFile.mapper.DbhsmIntegrityFileConfigMapper;
import com.spms.dbhsm.warningInfo.domain.DbhsmWarningInfo;
import com.spms.dbhsm.warningInfo.mapper.DbhsmWarningInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * <p> description: 报警定时任务加载 </p>
 *
 * <p> Powered by wzh On 2024-03-20 17:57 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Slf4j
@Component
public class DbhsmWarningJobLoad {

    //控制任务状态，保证线程安全
    private ConcurrentHashMap<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    @Autowired
    private DbhsmIntegrityFileConfigMapper fileConfigMapper;

    @Autowired
    private DbhsmWarningConfigMapper dbhsmWarningConfigMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmWarningInfoMapper dbhsmWarningInfoMapper;

    @Value("${integrityFile}")
    private String integrityFilePath;

    private static final String HMAC = "3";
    private static final String SM3 = "1";


    //创建定时任务执行对象
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    @Async
    @PostConstruct
    public void dataIntegrityJob() {
        try {

            List<DbhsmWarningConfigListResponse> jobList = dbhsmWarningConfigMapper.selectDbhsmWarningConfigList(new DbhsmWarningConfig());
            if (CollectionUtils.isEmpty(jobList)) {
                return;
            }

            /**
             * 1.查询数据库配置，获取：数据库连接信息、表、字段信息
             * 2.创建数据库连接根据表、字段取出数据
             * 3.取出数据进行SM3加密SM3Util.hash
             */

            for (DbhsmWarningConfigListResponse dbhsmWarningConfig : jobList) {

                if (1 == dbhsmWarningConfig.getEnableTiming()) {
                    if (futureMap.containsKey("data" + dbhsmWarningConfig.getId())) {
                        futureMap.remove("data" + dbhsmWarningConfig.getId()).cancel(false);
                    }
                    continue;
                }

                //数据库连接信息
                String databaseConnectionInfo = dbhsmWarningConfig.getDatabaseConnectionInfo();
                //需要校验的字段
                String tableFields = dbhsmWarningConfig.getTableFields();
                String[] split = tableFields.split("\\,");
                StringBuffer stringBuffer = new StringBuffer();
                for (String s : split) {
                    stringBuffer.append(s.trim()).append(",");
                }
                //需要校验的表信息
                String databaseTableInfo = dbhsmWarningConfig.getDatabaseTableInfo();
                //定时任务执行时间 x分钟
                String cron = dbhsmWarningConfig.getCron();
                //校验字段
                stringBuffer.append(dbhsmWarningConfig.getVerificationFields().trim());

                //先清空校验值列的数据
                connectionDelOldCheckValue(Long.parseLong(databaseConnectionInfo), dbhsmWarningConfig.getVerificationFields(), databaseTableInfo);
                Runnable task = () -> {
                    // 创建一个任务
                    connectionParam(Long.parseLong(databaseConnectionInfo), databaseTableInfo, stringBuffer.toString(), dbhsmWarningConfig.getId(), String.valueOf(dbhsmWarningConfig.getVerificationType()));
                };

                if (0 == dbhsmWarningConfig.getEnableTiming() && !futureMap.containsKey("data" + dbhsmWarningConfig.getId())) {
                    // 执行任务初始延迟1秒，然后每X分钟执行一次任务
                    ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(task, 1, Integer.parseInt(cron), TimeUnit.MINUTES);
                    futureMap.put("data" + dbhsmWarningConfig.getId(), scheduledFuture);
                }
            }

        } catch (Exception e) {
            log.error("定时任务调度失败：{}", e.getMessage());
        }
    }


    @Async
    @PostConstruct
    public void fileIntegrityJob() {
        try {
            List<DbhsmIntegrityFileConfig> dbhsmIntegrityFileConfigs = fileConfigMapper.selectDbhsmIntegrityFileConfigList(new DbhsmIntegrityFileConfig());

            if (CollectionUtils.isEmpty(dbhsmIntegrityFileConfigs)) {
                return;
            }


            //创建定时任务执行对象
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(dbhsmIntegrityFileConfigs.size());

            for (DbhsmIntegrityFileConfig fileConfig : dbhsmIntegrityFileConfigs) {
                if (1 == fileConfig.getEnableTiming()) {
                    if (futureMap.containsKey("file" + fileConfig.getId())) {
                        //删除任务
                        futureMap.remove("file" + fileConfig.getId()).cancel(false);
                    }
                    continue;
                }
                try {
                    String filePath = fileConfig.getFilePath();
                    File file = new File(integrityFilePath + File.separator + filePath);
                    //定时任务执行时间 x分钟
                    String cron = fileConfig.getCron();

                    //先清空校验值列的数据
                    fileConfig.setVerificationValue(null);
                    fileConfigMapper.updateDbhsmIntegrityFileConfig(fileConfig);
                    Runnable task = () -> {
                        // 创建一个任务
                        schedulerCheckFileJob(fileConfig, file);
                    };

                    if (0 == fileConfig.getEnableTiming() && !futureMap.containsKey("data" + fileConfig.getId())) {
                        // 执行任务初始延迟1秒，然后每X分钟执行一次任务
                        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(task, 1, Long.parseLong(cron), TimeUnit.MINUTES);
                        futureMap.put("file" + fileConfig.getId(), scheduledFuture);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("文件完整性定时任务启动失败：{}", e.getMessage());
        }
    }

    private void schedulerCheckFileJob(DbhsmIntegrityFileConfig fileConfig, File file) {
        if (!file.exists()) {
            log.error("文件信息不存在：{}", file.getPath());
            DbhsmWarningInfo dbhsmWarningInfo = new DbhsmWarningInfo();
            dbhsmWarningInfo.setStatus(0L);
            dbhsmWarningInfo.setResult("文件名为" + fileConfig.getFilePath() + "的文件已经被删除：");
            dbhsmWarningInfo.setOldVerificationValue(fileConfig.getVerificationValue());
            dbhsmWarningInfo.setCreateTime(new Date());
            dbhsmWarningInfoMapper.insertDbhsmWarningInfo(dbhsmWarningInfo);
            return;
        }

        String[] sha1sum = CommandUtil.exeCmd("sha1sum " + file.getPath()).split(" ");
        log.info("sha1计算值：{}", sha1sum[0]);

        String verification = verification(sha1sum[0].getBytes(), String.valueOf(fileConfig.getVerificationType()));

        if (StringUtils.isBlank(fileConfig.getVerificationValue())) {
            fileConfig.setVerificationValue(verification);
            fileConfigMapper.updateDbhsmIntegrityFileConfig(fileConfig);
            return;
        }

        if (!fileConfig.getVerificationValue().equals(verification)) {
            DbhsmWarningInfo dbhsmWarningInfo = new DbhsmWarningInfo();
            dbhsmWarningInfo.setStatus(1L);
            dbhsmWarningInfo.setResult("经校验：" + fileConfig.getFilePath() + "文件被篡改");
            dbhsmWarningInfo.setOldVerificationValue(fileConfig.getVerificationValue());
            dbhsmWarningInfo.setNewVerificationValue(verification);
            dbhsmWarningInfo.setCreateTime(new Date());
            dbhsmWarningInfoMapper.insertDbhsmWarningInfo(dbhsmWarningInfo);
            return;
        }

        log.info("数据一致，文件没有被篡改");
    }

    private String verification(byte[] srcData, String checkType) {
        if (HMAC.equals(checkType)) {
            //hmac加密
            BigInteger p = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF", 16);
            return Hex.toHexString(SM3Util.hmac(p.toByteArray(), srcData));
        }
        //sm3加密
        return Hex.toHexString(SM3Util.hash(srcData));
    }

    public void connectionParam(Long id, String table, String field, Long configId, String checkType) {
        System.out.println("任务" + table);
        //验证字段值
        String result = null;
        Connection conn = null;
        Statement stmt = null;
        Statement upStmt = null;
        ResultSet resultSet = null;
        String sql;
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (!ObjectUtils.isEmpty(instance)) {
            //创建数据库连接
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(instance, connDTO);
            try {
                conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                String instanceInfo = CommandUtil.getInstance(instance);
                if (Optional.ofNullable(conn).isPresent()) {
                    stmt = conn.createStatement();
                    sql = "SELECT " + field + " FROM " + table + ";";
                    resultSet = stmt.executeQuery(sql);
                    String[] split = field.split(",");
                    while (resultSet.next()) {
                        //校验字段 -- 数据
                        StringBuilder stringBuffer = new StringBuilder();
                        //result返回拼接数据
                        StringBuilder resultMsg = new StringBuilder();
                        DbhsmWarningInfo dbhsmWarningInfo = new DbhsmWarningInfo();
                        for (int i = 0; i < split.length - 1; i++) {
                            //校验列
                            stringBuffer.append(resultSet.getString(split[i]));
                            //result组装
                            resultMsg.append(split[i] + "=" + resultSet.getString(split[i]));
                        }
                        //获取最后一个字段值
                        result = resultSet.getString(split[split.length - 1]);
                        String verification = verification(stringBuffer.toString().getBytes(), checkType);
                        if (StringUtils.isBlank(result)) {
                            //更新数据 -- 条件字段
                            StringBuffer whereIsBuffer = new StringBuffer();
                            for (int i = 0; i < split.length - 1; i++) {
                                String where = null == resultSet.getString(split[i]) ? " is null" : " = '" + resultSet.getString(split[i]) + "'";
                                if (i != 0) {
                                    whereIsBuffer.append(" and ").append(split[i]).append(where);
                                    continue;
                                }
                                whereIsBuffer.append(" where ").append(split[i]).append(where);
                            }
                            //如果原校验值为空，更新原表数据
                            sql = "update " + table + " set " + split[split.length - 1] + "='" + verification + "'" + whereIsBuffer + ";";
                            System.out.println(sql);
                            upStmt = conn.createStatement();
                            int i = upStmt.executeUpdate(sql);
                            //提交事务
                            conn.commit();
                            log.info("原校验值为空，更新原校验值列数据：{}", i);
                            continue;
                        }
                        //如果校验值相同不需要更改
                        if (verification.equals(result)) {
                            continue;
                        }
                        //告警数据新增
                        dbhsmWarningInfo.setResult("连接信息：" + instanceInfo + "，表信息：" + table + "，经校验：[" + resultMsg + "]字段数据被篡改");
                        dbhsmWarningInfo.setStatus(1L);
                        dbhsmWarningInfo.setOldVerificationValue(result);
                        dbhsmWarningInfo.setNewVerificationValue(verification);
                        dbhsmWarningInfo.setCreateTime(new Date());
                        dbhsmWarningInfo.setConfigId(configId);
                        //数据库新增
                        dbhsmWarningInfoMapper.insertDbhsmWarningInfo(dbhsmWarningInfo);
                    }
                }
            } catch (SQLException | ZAYKException e) {
                DbhsmWarningInfo dbhsmWarningInfo = new DbhsmWarningInfo();
                dbhsmWarningInfo.setResult("数据库信息错误，当前表信息：" + table + "错误信息：" + e.getMessage());
                dbhsmWarningInfo.setStatus(0L);
                dbhsmWarningInfo.setCreateTime(new Date());
                dbhsmWarningInfo.setConfigId(configId);
                dbhsmWarningInfoMapper.insertDbhsmWarningInfo(dbhsmWarningInfo);
                log.error("任务同步异常：{}", e.getMessage());
            } finally {
                closeStatement(upStmt);
                closeConnection(conn);
                closeStatement(stmt);
                closeResultSet(resultSet);
            }
        }
    }

    //清空原验证值
    private void connectionDelOldCheckValue(Long id, String field, String table) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (!ObjectUtils.isEmpty(instance)) {
            //创建数据库连接
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(instance, connDTO);
            try {
                conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                if (Optional.ofNullable(conn).isPresent()) {
                    stmt = conn.createStatement();
                    String sql = "update " + table + " set " + field + "=NULL where " + field + " is null or " + field + " is not null";
                    int execute = stmt.executeUpdate(sql);
                    log.info("删除原校验值数据，执行sql返回值：{}", execute);
                    conn.commit();
                }
            } catch (ZAYKException | SQLException e) {
                log.error("校验前清除数据失败：{}", e.getMessage());
            } finally {
                closeConnection(conn);
                closeStatement(stmt);
                closeResultSet(resultSet);
            }
        }
    }

    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("关闭Connection失败：{}", e.getMessage());
            }
        }
    }

    public void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("关闭Statement失败：{}", e.getMessage());
            }
        }
    }

    public void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.error("关闭ResultSet失败：{}", e.getMessage());
            }
        }
    }

}
