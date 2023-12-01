package com.spms.common.dbTool.stockDataProcess.sqlserver;

import cn.hutool.core.util.ObjectUtil;
import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.CommandUtil;
import com.spms.common.dbTool.DBUtil;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

/**
 * @project ccsp
 * @description sqlserver存量数据加解密
 * @author 18853
 * @date 2023/12/1 15:40:53
 * @version 1.0
 */

@Slf4j
public class SqlServerStock {

    /**
     * sqlserver 存量数据加解密
     */
    public static void sqlserverStockEncOrDec(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, int encOrdecFlag) throws Exception {
        Statement statement = null;
        Map<String, String> schemaMap = DBUtil.findSchemaByTable(conn, dbhsmEncryptColumnsAdd.getDbTable());
        String schemaName = "dbo";
        if (schemaMap.containsKey("schemaName")) {
            schemaName = schemaMap.get("schemaName");
        }
        String policyId = dbhsmEncryptColumnsAdd.getId();
        String encryptColumns = dbhsmEncryptColumnsAdd.getEncryptColumns();
        String dbServerName = dbhsmEncryptColumnsAdd.getDatabaseServerName();
        String dbTable = dbhsmEncryptColumnsAdd.getDbTable();
        Integer encryptionOffset = ObjectUtil.isEmpty(dbhsmEncryptColumnsAdd.getEncryptionOffset()) ? 0 : dbhsmEncryptColumnsAdd.getEncryptionOffset();
        String algorithm = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String ipAndPort = dbhsmEncryptColumnsAdd.getIpAndPort();
        try {
            log.info("sqlserver存量数据加密start:\n");
            StringBuffer sqlBuffer = new StringBuffer();
            sqlBuffer.append("declare \n");
            sqlBuffer.append("@policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),\n");
            sqlBuffer.append("@db_instance_name NVARCHAR(50),@db_name NVARCHAR(50),@db_table_name NVARCHAR(50),\n");
            sqlBuffer.append("@db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),\n");
            sqlBuffer.append("@offset int,@length int,\n");
            sqlBuffer.append("@encryptdata nvarchar(max), @encryptdataLen int, \n");
            sqlBuffer.append("@decryptdata nvarchar(max), @decryptdataLen int,\n");
            sqlBuffer.append("@radix int,@algorithm int,@flag int\n");
            sqlBuffer.append("declare slc cursor for select " + encryptColumns + "  from " + dbServerName + "." + schemaName + "." + dbTable + " \n");
            sqlBuffer.append("open slc\n");
            sqlBuffer.append("fetch next from slc into @encryptdata\n");
            sqlBuffer.append("while(@@FETCH_STATUS=0)\n");
            sqlBuffer.append("\tBEGIN\n");
            sqlBuffer.append("\tset @policy_id = '" + policyId + "'\n");
            sqlBuffer.append("\tset @policy_url = 'http://" + ipAndPort + "/api/datahsm/v1/strategy/get'\n");
            sqlBuffer.append("\tselect @user_ipaddr = client_net_address FROM sys.dm_exec_connections WHERE session_id = @@SPID  \n");
            sqlBuffer.append("\tset @db_instance_name = CAST(@@ServerName as char)  --实例名\n");
            sqlBuffer.append("\tset @db_name = db_name()\n");
            sqlBuffer.append("\tset @db_table_name = '" + dbTable + "'\n");
            sqlBuffer.append("\tset @db_column_name = '" + encryptColumns + "'\n");
            sqlBuffer.append("\tset @db_user_name = suser_name()\n");
            sqlBuffer.append("\tset @encryptdataLen = DATALENGTH(@encryptdata)/2 ");
            sqlBuffer.append("\tset @offset = " + encryptionOffset + "\n");
            sqlBuffer.append("\tset @length = DATALENGTH(@encryptdata)/2  --加密长度，web配置0则设定为此表达式， 若配置1~6，则为6\n");
            sqlBuffer.append("\tset @decryptdataLen = DATALENGTH(@encryptdata)/2 \n");
            sqlBuffer.append("\tset @flag = " + encOrdecFlag + "\n");
            sqlBuffer.append("\twhile (@encryptdataLen is null)\n");
            sqlBuffer.append("\t  begin\n");
            sqlBuffer.append("\t    fetch next from slc into @encryptdata\n");
            sqlBuffer.append("\t    set @encryptdataLen = DATALENGTH(@encryptdata)/2 --加密长度，web配置0则设定为此表达式， 若配置1~6，则为6\n");
            sqlBuffer.append("\t    set @length = DATALENGTH(@encryptdata)/2\n");
            sqlBuffer.append("\t    set @decryptdataLen = DATALENGTH(@encryptdata)/2\n");
            sqlBuffer.append("\t  end\n");
            sqlBuffer.append("\tset @algorithm = " + algorithm + "; --算法 0-SM4； 10-FPE_10； 62-PFE_62；\n");
            sqlBuffer.append("\tif (@flag = 0)\n");
            sqlBuffer.append("\tbegin\n");
            sqlBuffer.append("  if (@algorithm = 0)\n");
            sqlBuffer.append("    begin\n");
            sqlBuffer.append("      set @decryptdata = dbtest.dbo.func_string_decrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("\t\t\t@db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("\t\t\t@encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen)\n");
            sqlBuffer.append("    end\n");
            sqlBuffer.append("  else\n");
            sqlBuffer.append("    begin\n");
            sqlBuffer.append("      set @radix = @algorithm;\n");
            sqlBuffer.append("      set @decryptdata = dbtest.dbo.func_fpe_decrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("\t\t\t@db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("\t\t\t@encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen, @radix)\n");
            sqlBuffer.append("    end\n");
            sqlBuffer.append(" end\n");
            sqlBuffer.append("else if (@flag = 1)\n");
            sqlBuffer.append("begin\n");
            sqlBuffer.append("if (@algorithm = 0)\n");
            sqlBuffer.append("\tbegin\n");
            sqlBuffer.append("\t\tset @decryptdata = dbtest.dbo.func_string_encrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("\t\t\t\t\t@db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("\t\t\t\t\t@encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen)\n");
            sqlBuffer.append("\tend\n");
            sqlBuffer.append("else\n");
            sqlBuffer.append("\tbegin\n");
            sqlBuffer.append("\t\tset @radix = @algorithm;\n");
            sqlBuffer.append("\t\tset @decryptdata = dbtest.dbo.func_fpe_encrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("\t\t\t\t\t@db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("\t\t\t\t\t@encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen, @radix)\n");
            sqlBuffer.append("\tend\n");
            sqlBuffer.append("end\n");
            sqlBuffer.append("\t\t if @decryptdata is null \n");
            sqlBuffer.append("\t\t break \n");
            sqlBuffer.append("\t\t update " + dbServerName + "." + schemaName + "." + dbTable + " set " + encryptColumns + " = @decryptdata where CURRENT OF slc --set XXX 加密列 \n");
            sqlBuffer.append("\t\t fetch next from slc into @encryptdata \n");
            sqlBuffer.append("\t END \n");
            sqlBuffer.append("CLOSE slc \n");
            sqlBuffer.append("DEALLOCATE slc\n");
            log.info("sqlserver存量数据加密 sql:" + sqlBuffer);
            statement = conn.createStatement();
            statement.execute(sqlBuffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    /**
     * 根据网口名称获取IP
     * @param ethernetPort
     * @return
     */
    private static String getIp(String ethernetPort) throws Exception {
        //获取端口
        String osName = System.getProperty("os.name");
        String ip = "";
        if (osName.toLowerCase().startsWith("linux")) {
            ip = CommandUtil.exeCmd("ip a| grep " + ethernetPort.split("@")[0] + " |grep inet |awk '{print $2}'|awk -F '/' '{print $1}'");
            if (StringUtils.isEmpty(ip)) {
                throw new Exception(ethernetPort + "口IP不存在,请先进行配置IP");
            }
            ip = ip.trim().replaceAll("\n", ",");
            if (StringUtils.isEmpty(ip)) {
                throw new Exception(ethernetPort + "口IP不存在,请先进行配置IP");
            }
            if (ip.lastIndexOf(",") == 0) {
                ip = ip.substring(0, ip.length() - 1);
            }

        }

        return ip.split(",")[0];
    }
}
