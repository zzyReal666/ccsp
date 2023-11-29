package com.spms.dbhsm.encryptcolumns.service.helper;

import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @project ccsp
 * @description 存量数据加解密
 * @author 18853
 * @date 2023/11/29 10:56:16
 * @version 1.0
 */
@Slf4j
public class EncryptionColumnsHelper {

    /**
     * 加密存量数据
     * create or replace function testuser1.tr_string_stock_testuser1_table1_name()
     * returns trigger as $$
     * begin
     *   NEW.name := testuser1.pgext_func_string_encrypt(
     *  '443970420010520576', --确保与配置加密列所设置的pid相同
     *  'http://192.168.6.88:10013/api/datahsm/v1/strategy/get',  --确保与配置加密列所设置的url相同
     *   CAST(inet_client_addr() as text),
     *  --ip
     *   CAST(current_catalog as text),
     *  --实例名
     *   CAST(current_catalog as text),
     *  --库名
     *  'TABLE1',
     *   --表名
     *  'name',
     *   --列名，以列名 name 为例
     *  CAST(user as text),
     *   --用户名
     *  OLD.name,0,0);
     *   --OLD.name 加密列， --0 offset --0 加密长度，0为默认加密全部;
     *  return NEW;
     * end; $$
     * language 'plpgsql';
     */
    public static void encryptionExistingData(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user) throws Exception {
        Statement statement = null;
        String  alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        //String userSchema = "'"+user.getDbSchema()+"'";
        String userSchema = user.getDbSchema();
        userSchema = user.getDbSchema();
        String funName = "\""+userSchema + "\".tr_" + DbConstants.algMappingStrOrFpe(alg) + "_stock_" + user.getUserName() + "_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();
        String funName$ = "tr_" + user.getUserName() + "_"  + userSchema + "_"  + dbhsmEncryptColumnsAdd.getDbTable() + "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();
        try {
            // 1、定义触发器函数
            log.info("创建PostgreSql触发器函数start");
            //函数名是动态的
            StringBuffer transFun = new StringBuffer("create or replace function " + funName + "()");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("returns trigger as $$");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("begin");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + " := "+ userSchema +".pgext_func_"+DbConstants.algMappingStrOrFpe(alg)+"_encrypt(");
            transFun.append("'" + dbhsmEncryptColumnsAdd.getId() + "',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("'http://" + dbhsmEncryptColumnsAdd.getIpAndPort()+ "/api/datahsm/v1/strategy/get',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(inet_client_addr() as text),");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(current_catalog as text),");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(current_catalog as text),");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("'" + dbhsmEncryptColumnsAdd.getDbTable() + "',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("'" + dbhsmEncryptColumnsAdd.getEncryptColumns() + "',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(user AS text),");
            transFun.append(System.getProperty("line.separator"));

            if (DbConstants.SGD_SM4.equals(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm())) {
                transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + ",0,0);\n");
            } else {
                if (DbConstants.ESTABLISH_RULES_YES.equals(dbhsmEncryptColumnsAdd.getEstablishRules())) {
                    transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + "," + //加密列
                            (dbhsmEncryptColumnsAdd.getEncryptionOffset() -1 ) + "," + //偏移量
                            (dbhsmEncryptColumnsAdd.getEncryptionLength()-(dbhsmEncryptColumnsAdd.getEncryptionOffset() -1 )) +"," +//加密长度
                            dbhsmEncryptColumnsAdd.getEncryptionAlgorithm() + ");\n");
                } else {
                    transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + ",0,0,"+ dbhsmEncryptColumnsAdd.getEncryptionAlgorithm() +");\n");
                }
            }
            transFun.append(System.getProperty("line.separator"));
            transFun.append("return NEW;");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("end");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("$" + funName$ + "$ language 'plpgsql'");
            transFun.append(System.getProperty("line.separator"));

            log.info("exec sql:" + transFun);
            statement = conn.createStatement();
            statement.execute(transFun.toString());

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }
}
