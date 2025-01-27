package com.spms.common;

import cn.hutool.core.util.RuntimeUtil;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.*;

@Slf4j
public class CommandUtil {

    public static String getInstance(DbhsmDbInstance instance) {
        String databaseType = "";
        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_ORACLE_DESC;
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_SQLSERVER_DESC;
        } else if (DbConstants.DB_TYPE_MYSQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_MYSQL_DESC;
        } else if(DbConstants.DB_TYPE_POSTGRESQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_POSTGRESQL_DESC;
        }else if(DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_DM_DESC;
        }else if(DbConstants.DB_TYPE_CLICKHOUSE.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_CLICKHOUSE_DESC;
        }else if(DbConstants.DB_TYPE_KB.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_KING_BASE_DESC;
        }else if(DbConstants.DB_TYPE_HB.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_HBASE_DESC;
        }
        return databaseType + ":" + instance.getDatabaseIp() + ":" + instance.getDatabasePort() + instance.getDatabaseExampleType() + instance.getDatabaseServerName();
    }
    public static String excCommand(String command) {
        return exeCmd(command, 10);
    }
    public static String exeCmd(String commandStr) {

        String result = null;
        try {
            String[] cmd = new String[]{"/bin/sh", "-c", commandStr};
            Process ps = Runtime.getRuntime().exec(cmd);

            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                //执行结果加上回车
                sb.append(line).append("\n");
            }
            result = sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        return result;

    }
    public static String exeCmd(String commandStr, int outTime) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //接收异常结果流
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            CommandLine commandline = CommandLine.parse(commandStr);
            DefaultExecutor exec = new DefaultExecutor();
            exec.setExitValues(null);
            //设置一分钟超时
            ExecuteWatchdog watchdog = new ExecuteWatchdog(outTime * 1000);
            exec.setWatchdog(watchdog);
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandline);
            //不同操作系统注意编码，否则结果乱码
            String out = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");

            return out + error;
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
    }
    /**
     * 执行系统命令，使用系统默认编码
     *     Params:
     *     cmds – 命令列表，每个元素代表一条命令
     *     Returns:
     *     执行结果
    */
    public static String execCmd(String command) {
        return RuntimeUtil.execForStr("sh", "-c", command);
    }

    public static String excCommandRT(String command) {
        return RuntimeUtil.execForStr(new String[]{"sh", "-c", command});
    }
    /**文件是否有可执行权限，没有则赋可执行权*/
    public static void checkFileCanExecute(String filePath){
        File file = new File(filePath);
        if (file.exists() && !file.canExecute()) {
            file.setExecutable(true);
        }
    }
    public static StringBuffer execProcessssl(String command) {


        StringBuffer result = new StringBuffer();
        try {

            Process p= Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader br= new BufferedReader(new InputStreamReader( p.getInputStream()));
            String line = null;

            while(null!=(line=br.readLine()))
            {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


}
