package com.spms.common.dbTool.stockDataProcess.sqlserver;

import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.CommandUtil;
import com.spms.common.constant.DbConstants;
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
        long encLength = 0;
        Map<String, String> schemaMap = DBUtil.findSchemaByTable(conn, dbhsmEncryptColumnsAdd.getDbTable());
        String schemaName = "dbo";
        if (schemaMap.containsKey("schemaName")) {
            schemaName = schemaMap.get("schemaName");
        }
        String policyId = dbhsmEncryptColumnsAdd.getId();
        String encryptColumns = dbhsmEncryptColumnsAdd.getEncryptColumns();
        String dbServerName = dbhsmEncryptColumnsAdd.getDatabaseServerName();
        String dbTable = dbhsmEncryptColumnsAdd.getDbTable();
        Integer establishRules = dbhsmEncryptColumnsAdd.getEstablishRules();
        //是否配置加密规则
        boolean haveRules =DbConstants.ESTABLISH_RULES_YES.equals(establishRules);
        //加密长度
        if(haveRules) {
            encLength = dbhsmEncryptColumnsAdd.getEncryptionLength()-(dbhsmEncryptColumnsAdd.getEncryptionOffset()-1);
        }
        //偏移量：未配置加密规则偏移量取0
        int encryptionOffset = haveRules ? dbhsmEncryptColumnsAdd.getEncryptionOffset()-1 : 0;
        String algorithm = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String ipAndPort = dbhsmEncryptColumnsAdd.getIpAndPort();
        try {
            log.info("sqlserver存量数据"+(encOrdecFlag== DbConstants.ENC_FLAG? "加密":"解密")+"start:\n");
            StringBuffer sqlBuffer = new StringBuffer();
            sqlBuffer.append("declare \n");
            sqlBuffer.append("@policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),\n");
            sqlBuffer.append("@db_instance_name NVARCHAR(50),@db_name NVARCHAR(50),@db_table_name NVARCHAR(50),\n");
            sqlBuffer.append("@db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),\n");
            sqlBuffer.append("@offset int,@length int,\n");
            sqlBuffer.append("@encryptdata nvarchar(max), @encryptdataLen int, \n");
            sqlBuffer.append("@decryptdata nvarchar(max), @decryptdataLen int,\n");
            sqlBuffer.append("@radix int,@algorithm int,@flag int,\n");
            sqlBuffer.append("@break1 int set @break1 = 0 --跳出循环标志位\n");
            sqlBuffer.append("declare slc cursor for select ").append(encryptColumns).append("  from ").append(dbServerName).append(".").append(schemaName).append(".").append(dbTable).append(" \n");
            sqlBuffer.append("open slc\n");
            sqlBuffer.append("fetch next from slc into @encryptdata\n");
            sqlBuffer.append("while(@@FETCH_STATUS=0)\n");
            sqlBuffer.append(" BEGIN\n");
            sqlBuffer.append(" set @policy_id = '").append(policyId).append("'\n");
            sqlBuffer.append(" set @policy_url = 'http://").append(ipAndPort).append("/prod-api/dbhsm/api/datahsm/v1/strategy/get'\n");
            sqlBuffer.append(" select @user_ipaddr = client_net_address FROM sys.dm_exec_connections WHERE session_id = @@SPID  \n");
            sqlBuffer.append(" set @db_instance_name = CAST(@@ServerName as char)  --实例名\n");
            sqlBuffer.append(" set @db_name = db_name()\n");
            sqlBuffer.append(" set @db_table_name = '").append(dbTable).append("'\n");
            sqlBuffer.append(" set @db_column_name = '").append(encryptColumns).append("'\n");
            sqlBuffer.append(" set @db_user_name = suser_name()\n");
            sqlBuffer.append(" set @encryptdataLen = LEN(@encryptdata) --待运算数据长度\n");
            sqlBuffer.append(" set @offset = ").append(encryptionOffset).append("\n");
            sqlBuffer.append(" set @length = ").append(haveRules ? encLength : "LEN(@encryptdata)").append(" --加密长度，web配置0则设定为此表达式， 若配置1~6，则为6\n");
            sqlBuffer.append(" set @decryptdataLen =  LEN(@encryptdata)\n");
            sqlBuffer.append(" set @flag = ").append(encOrdecFlag).append("--0解密 1加密\n");
            sqlBuffer.append(" while (@encryptdataLen is null or @encryptdataLen < 6 or @encryptdataLen < (@length + @offset))\n");
            sqlBuffer.append("   begin\n");
            sqlBuffer.append("     fetch next from slc into @encryptdata\n");
            sqlBuffer.append("     if(@@FETCH_STATUS!=0) --获取到最后一行数据为null，直接退出\n");
            sqlBuffer.append("     begin\n");
            sqlBuffer.append("       set @break1 = 1\n");
            sqlBuffer.append("       break\n");
            sqlBuffer.append("     end\n");
            sqlBuffer.append("     set @encryptdataLen = LEN(@encryptdata) --待运算数据长度\n");
            sqlBuffer.append("     set @length = ").append(haveRules ? encLength : "LEN(@encryptdata)").append(" -- * 加密长度，与web配置的长度保持一致\n ");
            sqlBuffer.append("     set @decryptdataLen = LEN(@encryptdata) --待运算数据长度\n");
            sqlBuffer.append("   end\n");
            sqlBuffer.append("   if(@break1 = 1)\n");
            sqlBuffer.append("   begin\n");
            sqlBuffer.append("     break\n");
            sqlBuffer.append("   end\n");
            sqlBuffer.append(" set @algorithm = ").append(algorithm).append("; --算法 0-SM4； 10-FPE_10； 62-PFE_62；\n");
            sqlBuffer.append("if (@flag = 0)\n");
            sqlBuffer.append("begin\n");
            sqlBuffer.append("  if (@algorithm = 0)\n");
            sqlBuffer.append("    begin\n");
            sqlBuffer.append("      set @decryptdata = "+dbServerName+"."+schemaName+".func_string_decrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("   @db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("   @encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen)\n");
            sqlBuffer.append("    end\n");
            sqlBuffer.append("  else\n");
            sqlBuffer.append("    begin\n");
            sqlBuffer.append("      set @radix = @algorithm;\n");
            sqlBuffer.append("      set @decryptdata = "+dbServerName+"."+schemaName+".func_fpe_decrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("   @db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("   @encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen, @radix)\n");
            sqlBuffer.append("    end\n");
            sqlBuffer.append(" end\n");
            sqlBuffer.append("else if (@flag = 1)\n");
            sqlBuffer.append("begin\n");
            sqlBuffer.append("if (@algorithm = 0)\n");
            sqlBuffer.append(" begin\n");
            sqlBuffer.append("  set @decryptdata = "+dbServerName+"."+schemaName+".func_string_encrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("     @db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("     @encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen)\n");
            sqlBuffer.append(" end\n");
            sqlBuffer.append("else\n");
            sqlBuffer.append(" begin\n");
            sqlBuffer.append("  set @radix = @algorithm;\n");
            sqlBuffer.append("  set @decryptdata = "+dbServerName+"."+schemaName+".func_fpe_encrypt_ex(@policy_id, @policy_url, @user_ipaddr, \n");
            sqlBuffer.append("     @db_instance_name, @db_name, @db_table_name, @db_column_name, @db_user_name,\n");
            sqlBuffer.append("     @encryptdata, @encryptdataLen, @offset, @length, @decryptdataLen, @radix)\n");
            sqlBuffer.append(" end\n");
            sqlBuffer.append("end\n");
            sqlBuffer.append("   if @decryptdata is null \n");
            sqlBuffer.append("   break \n");
            sqlBuffer.append("   update ").append(dbServerName).append(".").append(schemaName).append(".").append(dbTable).append(" set ").append(encryptColumns).append(" = @decryptdata where CURRENT OF slc --set XXX 加密列 \n");
            sqlBuffer.append("   fetch next from slc into @encryptdata \n");
            sqlBuffer.append("  END \n");
            sqlBuffer.append("CLOSE slc \n");
            sqlBuffer.append("DEALLOCATE slc\n");
            log.info("sqlserver存量数据"+(encOrdecFlag== DbConstants.ENC_FLAG? "加密":"解密")+" sql:" + sqlBuffer);
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
