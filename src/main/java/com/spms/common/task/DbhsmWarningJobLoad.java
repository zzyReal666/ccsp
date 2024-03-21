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
import com.spms.dbhsm.warningInfo.domain.DbhsmWarningInfo;
import com.spms.dbhsm.warningInfo.mapper.DbhsmWarningInfoMapper;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p> description: 报警定时任务加载 </p>
 *
 * <p> Powered by wzh On 2024-03-20 17:57 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Component
public class DbhsmWarningJobLoad {

    private ScheduledExecutorService scheduler;

    @Autowired
    private DbhsmWarningConfigMapper dbhsmWarningConfigMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmWarningInfoMapper dbhsmWarningInfoMapper;


    @Async
    @PostConstruct
    public void initLoading() {

        List<DbhsmWarningConfig> dbhsmWarningConfigs = dbhsmWarningConfigMapper.selectDbhsmWarningConfigList(new DbhsmWarningConfig());

        List<DbhsmWarningConfig> jobList = dbhsmWarningConfigs.stream().filter(dbhsmWarningConfig -> 0 == dbhsmWarningConfig.getEnableTiming()).collect(Collectors.toList());

        /**
         * 1.查询数据库配置，获取：数据库连接信息、表、字段信息
         * 2.创建数据库连接根据表、字段取出数据
         * 3.取出数据进行SM3加密SM3Util.hash
         */
        //任务数
        this.scheduler = Executors.newScheduledThreadPool(jobList.size());
        for (DbhsmWarningConfig dbhsmWarningConfig : jobList) {
            //数据库连接信息
            String databaseConnectionInfo = dbhsmWarningConfig.getDatabaseConnectionInfo();
            //需要校验的字段
            String tableFields = dbhsmWarningConfig.getTableFields();
            String[] split = tableFields.split("\\,");
            StringBuffer stringBuffer = new StringBuffer();
            for (String s : split) {
                stringBuffer.append("").append(s).append(",");
            }
            //需要校验的表信息
            String databaseTableInfo = dbhsmWarningConfig.getDatabaseTableInfo();
            //定时任务执行时间 x分钟
            String cron = dbhsmWarningConfig.getCron();

            //校验字段
            stringBuffer.append(dbhsmWarningConfig.getVerificationFields());
            Runnable task = () -> {
                // 创建一个任务
                connectionParam(Long.parseLong(databaseConnectionInfo), databaseTableInfo, stringBuffer.toString());
            };

            // 执行任务初始延迟1秒，然后每X分钟执行一次任务
            scheduleTask(task, 1, Integer.parseInt(cron), TimeUnit.MINUTES);
        }
    }

    private String verification(String value) {
        byte[] srcData = value.getBytes();
        return Hex.toHexString(SM3Util.hash(srcData));
    }

    public void connectionParam(Long id, String table, String field) {
        String result = null;
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        String sql;
        StringBuffer stringBuffer = new StringBuffer();
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (!ObjectUtils.isEmpty(instance)) {
            DbhsmWarningInfo dbhsmWarningInfo = new DbhsmWarningInfo();
            //创建数据库连接
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(instance, connDTO);
            try {
                conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                if (Optional.ofNullable(conn).isPresent()) {
                    stmt = conn.createStatement();
                    sql = "SELECT " + field + " FROM " + table + " limit 1;";
                    resultSet = stmt.executeQuery(sql);
                    String[] split = field.split(",");
                    while (resultSet.next()) {
                        for (int i = 0; i < split.length - 1; i++) {
                            stringBuffer.append(resultSet.getString(split[i]));
                        }
                        //获取最后一个字段值
                        result = resultSet.getString(split[split.length - 1]);
                    }
                    String verification = verification(stringBuffer.toString());
                    //如果校验值相同不需要更改
                    if (verification.equals(result)) {
                        System.out.println("校验一致,数据没有被更改");
                        return;
                    }
                    String instanceInfo = CommandUtil.getInstance(instance);
                    //告警数据新增
                    dbhsmWarningInfo.setResult("连接信息：" + instanceInfo + "，经校验：[" + field + "]字段数据被篡改");
                    dbhsmWarningInfo.setStatus(1L);
                    dbhsmWarningInfo.setOldVerificationValue(result);
                    dbhsmWarningInfo.setNewVerificationValue(verification);
                    dbhsmWarningInfo.setCreateTime(new Date());

                }
            } catch (SQLException | ZAYKException e) {
                dbhsmWarningInfo.setResult(e.getMessage());
                dbhsmWarningInfo.setStatus(0L);
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
                dbhsmWarningInfoMapper.insertDbhsmWarningInfo(dbhsmWarningInfo);
            }
        }
    }

    public void scheduleTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        // 取消之前可能存在的相同任务（如果有的话）
        scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

}
