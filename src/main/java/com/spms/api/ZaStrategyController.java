package com.spms.api;

import cn.hutool.core.util.ObjectUtil;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.PaddUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.redis.service.RedisService;
import com.ccsp.system.api.hsmSvsTsaApi.RemoteSecretKeyService;
import com.spms.common.JSONDataUtil;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.service.IDbhsmDbInstanceService;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.service.IDbhsmEncryptColumnsService;
import com.spms.dbhsm.permission.service.IDbhsmPermissionService;
import com.spms.dbhsm.secretKey.domain.DbhsmSecretKeyManage;
import com.spms.dbhsm.secretKey.service.IDbhsmSecretKeyManageService;
import com.spms.dbhsm.secretService.service.IDbhsmSecretServiceService;
import com.zayk.ciphercard.ZaykManageClass;
import com.zayk.ciphercard.factory.zayk.ZaSDSUtils;
import com.zayk.ciphercard.util.BitOperator;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.*;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author diq
 * @ClassName ZaStrategyController
 * @date 2022-11-30
 */
@Slf4j
@Controller()
@RequestMapping("/api/datahsm/v1/strategy")
public class ZaStrategyController {

    @Autowired
    IDbhsmDbInstanceService dbInstanceService;
    @Autowired
    IDbhsmPermissionService permissionService;
    @Autowired
    IDbhsmSecretServiceService serviceService;
    @Autowired
    IDbhsmSecretKeyManageService secretKeyManageService;
    @Autowired
    RemoteSecretKeyService secretKeyService;

    @Autowired
    private IDbhsmEncryptColumnsService encryptColumnsService;

    @Autowired
    private RedisService redisService;

    static int SDR_SYS_MASTER_KEY_NO_EXIST = 0x1200008;
    private static ZaykManageClass mgr = new ZaykManageClass();
    private static int crytoCartTypeSInt = 0;

    public static String ALGORITHM_TYPE_SYK = "SYK";

    static {
        getCrytoCartType();
    }
    private static void getCrytoCartType() {
        try {
            Object crytoCartType = JSONDataUtil.getSysDataToDB(DbConstants.cryptoCardType);
            if(crytoCartType == null ){
                log.info("获取到密钥卡类型为空：{}",crytoCartType);
                return;
            }
            if(DbConstants.CRYPTO_CARD_TYPE_NO_CARD.equals(crytoCartType.toString())){
                log.info("密码卡类型为0，不进行密码卡初始化：{}",crytoCartType);
                return;
            }
            crytoCartTypeSInt = Integer.parseInt(crytoCartType.toString());
            mgr.Initialize(crytoCartTypeSInt);
        } catch (Exception e) {
            log.error("系统模块连接异常，5秒后进行重连");
            try {
                Thread.sleep(1000 * 5);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            getCrytoCartType();
        }
    }

    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        int len;
        while ((len = inStream.read(buffer)) != -1) {
            outSteam.write(buffer, 0, len);
        }

        outSteam.close();
        inStream.close();
        return outSteam.toByteArray();
    }

    @RequestMapping(value = "/get", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public void get(HttpServletRequest request, HttpServletResponse response) {
        log.info("请求开始,请求方式：" + request.getMethod());
        try {
            byte[] requestApiDataByte = readInputStream(request.getInputStream());
            if (requestApiDataByte != null && requestApiDataByte.length > 0) {
                log.info("request param:" + Base64.toBase64String(requestApiDataByte));
                getStrategyFile(requestApiDataByte, response);
                return;
            }
            log.info("调用默认");
            response.setHeader("Content-Type", "application/octet-stream");

            ASN1EncodableVector v = new ASN1EncodableVector();
            //char db_url[256];
//                int policy_type;            //默认0
            v.add(new ASN1Integer(DbConstants.POLICY_TYPE_PUSH));
//                int policy_version;         //默认0
            v.add(new ASN1Integer(DbConstants.POLICY_VERSION));
//                int db_type;                //数据库类型，
            v.add(new ASN1Integer(DbConstants.DB_TYPE_ORACLE_api));
//                char db_version[128];       //数据库版本
            v.add(new DEROctetString("Oracle12c 12.2.0.1.0(64-bit)".getBytes()));
//                char db_url[256];           //数据库地址
            v.add(new DEROctetString("192.168.7.129".getBytes()));
//                int db_port;                //端口,默认1521
            v.add(new ASN1Integer(1521));
            //实例类型：SID取值 ":" ；服务名取值 "/"
            v.add(new DEROctetString("/".getBytes()));
//                char db_name[128];          //数据库名称
            v.add(new DEROctetString("orclpdb".getBytes()));
//                char db_table_name[128];    //表名
            v.add(new DEROctetString("TABLE2".getBytes()));
//                char db_column_name[128];   //列名
            v.add(new DEROctetString("COLUMN1".getBytes()));
//                char db_column_type;        //列类型
            v.add(new ASN1Integer(DbConstants.getColumnTypeToInt("VARCHAR2")));
            //                char db_user_name[128];     //用户名
            v.add(new DEROctetString("superAdmin".getBytes()));
//                char db_user_passwd[128];   //用户口令
            v.add(new DEROctetString("12345678".getBytes()));

//                char key_id[256];           //kmip相关
            v.add(new DEROctetString("kmip相关".getBytes()));
//                char key_auth_code[256];    //kmip相关
            v.add(new DEROctetString("kmip相关".getBytes()));
//
//                char key[32];               // 对称密钥
            byte[] key = Base64.decode("rNe0Mwty2E+AfNotLSi9sw==");
            v.add(new DEROctetString(key));
//                char iv[32];                // 初始化向量
            v.add(new DEROctetString(new byte[16]));
//                int cipher_type;            // SGD_SM4_CTR
            v.add(new ASN1Integer(DbConstants.SGD_SM4_CTR));
//                int hash_type;              // SGD_SM3
            v.add(new ASN1Integer(DbConstants.SGD_SM3));
//            } POLICY_INFO;

            // 5.创建数据缓冲区
            byte[] buffer = new DERSequence(v).getEncoded();
            // 6.通过response对象获取OutputStream流
            OutputStream out = response.getOutputStream();
            // 8.使用OutputStream将缓冲区的数据输出到客户端浏览器
            out.write(buffer, 0, buffer.length);
        } catch (Exception e) {
            e.printStackTrace();
            ASN1EncodableVector v = new ASN1EncodableVector();
            String msg = "系统异常: " + e.getMessage();
            v.add(new DEROctetString(msg.getBytes()));
            response.setStatus(500);
            try {
                OutputStream out = response.getOutputStream();
                out.write(new DERSequence(v).getEncoded());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public void getStrategyFile(byte[] requestApiDataByte, HttpServletResponse response) {
        DbhsmDbInstance dbInstance;
        response.setHeader("Content-Type", "application/octet-stream");
        try {
            //获取查询条件信息
            RequestApiData requestApiData = getRequestApiData(requestApiDataByte);
            //根据查询条件获取加密列信息
            DbhsmEncryptColumns dbEncryptColumns = getEncryptColumnsInfo(requestApiData);
            //根据查询条件查询数据库实例信息 获取数据库版本
            dbInstance = getDbInstanceInfo(dbEncryptColumns.getDbInstanceId());

            //组装策略信息
            ASN1EncodableVector strategyInfo = getStrategyInfo(requestApiData, dbEncryptColumns, dbInstance);
            // 5.创建数据缓冲区
            byte[] buffer = new DERSequence(strategyInfo).getEncoded();
            // 6.通过response对象获取OutputStream流
            OutputStream out = response.getOutputStream();
            // 8.使用OutputStream将缓冲区的数据输出到客户端浏览器
            out.write(buffer, 0, buffer.length);
        } catch (ZAYKException e) {
            e.printStackTrace();
            String msg = "调用异常: " + e.getMessage();
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new DEROctetString(msg.getBytes()));
            response.setStatus(500);
            try {
                OutputStream out = response.getOutputStream();
                out.write(new DERSequence(v).getEncoded());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            String msg = "IO异常: " + e.getMessage();
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new DEROctetString(msg.getBytes()));
            response.setStatus(500);
            try {
                OutputStream out = response.getOutputStream();
                out.write(new DERSequence(v).getEncoded());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param requestApiData
     * @param dbInstance
     * @return
     */
    private ASN1EncodableVector getStrategyInfo(RequestApiData requestApiData, DbhsmEncryptColumns dbEncryptColumns, DbhsmDbInstance dbInstance) throws Exception {
        String symmKey;

        symmKey = getSymmKey(dbEncryptColumns);
        ASN1EncodableVector v = new ASN1EncodableVector();
        //                int policy_type;            //默认0
        v.add(new ASN1Integer(DbConstants.POLICY_TYPE_PUSH));
//                int policy_version;         //默认0
        v.add(new ASN1Integer(DbConstants.POLICY_VERSION));
        if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(dbInstance.getDatabaseType())){
            //                int db_type;                //数据库类型，
            v.add(new ASN1Integer(DbConstants.DB_TYPE_ORACLE_api));
            //char db_version[128];       //数据库版本
            v.add(new DEROctetString(requestApiData.getDatabaseEdition().getBytes()));
        }else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(dbInstance.getDatabaseType())){
            //                int db_type;                //数据库类型，
            v.add(new ASN1Integer(DbConstants.DB_TYPE_MSSQL_api));
            //char db_version[128];       //数据库版本
            v.add(new DEROctetString(dbInstance.getDatabaseEdition().getBytes()));
        }else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(dbInstance.getDatabaseType())){
            //                int db_type;                //数据库类型，
            v.add(new ASN1Integer(DbConstants.DB_TYPE_MYSQL_api));
            //char db_version[128];       //数据库版本
            v.add(new DEROctetString(dbInstance.getDatabaseEdition().getBytes()));
        }else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(dbInstance.getDatabaseType())){
            //                int db_type;                //数据库类型，
            v.add(new ASN1Integer(DbConstants.DB_TYPE_POSTGRESQL_api));
            //char db_version[128];       //数据库版本
            v.add(new DEROctetString(dbInstance.getDatabaseEdition().getBytes()));
        }

        //char db_url[256];           //数据库地址
        v.add(new DEROctetString(dbInstance.getDatabaseIp().getBytes()));
        //int db_port;                //端口,默认1521
        v.add(new ASN1Integer(dbInstance.getDatabasePort().getBytes()));
        //char db_name[128];          //数据库实例名
        v.add(new DEROctetString(requestApiData.getDatabaseServerName().getBytes()));
        //char db_name[128];          //数据库名称
        v.add(new DEROctetString(requestApiData.getDatabaseServerName().getBytes()));
        //char db_table_name[128];    //表名
        v.add(new DEROctetString(requestApiData.getDbTableName().getBytes()));
        //char db_column_name[128];   //列名
        v.add(new DEROctetString(requestApiData.getEncryptColumns().getBytes()));
        //int db_column_type;        //列类型
        v.add(new ASN1Integer(DbConstants.getColumnTypeToInt(dbEncryptColumns.getColumnsType())));
        //char db_user_name[128];     //用户名
        v.add(new DEROctetString(requestApiData.getUserName().getBytes()));
        //char db_user_passwd[128];   //用户口令
        v.add(new DEROctetString(dbInstance.getDatabaseDbaPassword().getBytes()));

        //char key_id[256];           //kmip相关
        v.add(new DEROctetString("kmip相关".getBytes()));
        //char key_auth_code[256];    //kmip相关
        v.add(new DEROctetString("kmip相关".getBytes()));

        //char key[32];               // 对称密钥
        byte[] key = Base64.decode(symmKey);
        v.add(new DEROctetString(key));
        //char iv[32];                // 初始化向量
        v.add(new DEROctetString(new byte[16]));
        //int cipher_type;            // SGD_SM4_CTR
        v.add(new ASN1Integer(DbConstants.SGD_SM4_CTR));
        //int hash_type;              // SGD_SM3
        v.add(new ASN1Integer(DbConstants.SGD_SM3));
        //LICY_INFO;
        return v;
    }

    private String getSymmKey(DbhsmEncryptColumns dbEncryptColumns) throws Exception {
        String symmKey = null;
        //根据加密列信息获取加密列使用的密钥ID,根据密钥ID查询密钥信息（za_secret_key_manage表中的密钥关系信息），根据密钥信息获取该密钥的密钥来源
        //密钥ID，对应密钥表中的id字段
        String keyId = dbEncryptColumns.getSecretKeyId();
        DbhsmSecretKeyManage dbhsmSecretKeyManage = new DbhsmSecretKeyManage();
        dbhsmSecretKeyManage.setSecretKeyId(keyId);
        List<DbhsmSecretKeyManage> secretKeyManageList = secretKeyManageService.selectDbhsmSecretKeyManageList(dbhsmSecretKeyManage);
        if (StringUtils.isEmpty(secretKeyManageList)){
            throw new Exception("未获取到密钥信息:密钥id:" + keyId);
        }
        DbhsmSecretKeyManage secretKeyManage = secretKeyManageList.get(0);
        //密钥索引
        Long secretKeyIndex = secretKeyManage.getSecretKeyIndex();
        //密钥来源：
        Integer keySource = secretKeyManage.getSecretKeySource();

        Object crytoCartTypeObj = JSONDataUtil.getSysDataToDB(DbConstants.cryptoCardType);
        if (null == crytoCartTypeObj) {
            throw new IOException();
        }

        int crytoCartTypeSInt = Integer.parseInt(crytoCartTypeObj.toString());
        ZaykManageClass mgr = new ZaykManageClass();
        Integer ret = mgr.Initialize(crytoCartTypeSInt);
        if (DbConstants.KEY_SOURCE_SECRET_CARD.equals(keySource)) {
            int keyGenerationMode = JSONDataUtil.getSecretKeyGenerateType(DbConstants.SYSDATA_ALGORITHM_TYPE_SYK);
            if (keyGenerationMode == DbConstants.HARD_SECRET_KEY) {
                //密钥来源为密码卡，导出密钥
                Map<String, String> exportKey = mgr.ZaykExportKeyPair(secretKeyManage.getSecretKeyType(), secretKeyIndex.intValue());
                if (null == exportKey) {
                    log.error("加密列配置的" + secretKeyIndex + "号对称密钥不存在,请重新生成密钥。");
                    throw new IOException();
                }
                symmKey = exportKey.get("privateKey");
            }else {
                //大容量密钥从数据库获取
                //R<HsmSymmetricSecretKey> symmetricSecretKeyR = secretKeyService.selectSymSecretKeyInfo(Math.toIntExact(secretKeyIndex));
                //if (ObjectUtil.isEmpty(symmetricSecretKeyR.getData())) {
                //    throw new Exception("获取密钥失败！");
                //}
                //if( StringUtils.isEmpty(symmetricSecretKeyR.getData().getPrivateKey())){
                //    throw new Exception("获取密钥为空");
                //}
                //symmKey = symmetricSecretKeyR.getData().getPrivateKey();
                ////主密钥解密
                //symmKey = SMKDecryptMethod(symmKey);
                //大容量密钥从redis获取
                String symmKeyIndex = getCacheNameDefaultSym(ALGORITHM_TYPE_SYK, Math.toIntExact(secretKeyIndex));
                Map<String, Object> cachedMap  = redisService.getCacheObject(symmKeyIndex);
                symmKey = cachedMap.get("symKey").toString();
            }
        } else if(DbConstants.KEY_SOURCE_KMIP.equals(keySource)) {
            //密钥来源KMIP
            String keyMaterial = secretKeyManage.getSecretKey();
            if (StringUtils.isEmpty(keyMaterial)){
                //重新从KMIP中获取
                keyMaterial = getKMIPKey(secretKeyManage);
            }
            if (StringUtils.isEmpty(keyMaterial)){
                throw new Exception("获取密钥为空");
            }

            //获取数字信封
            ASN1Sequence root = (ASN1Sequence) ASN1Sequence.fromByteArray(Base64.decode(keyMaterial));
            ASN1Sequence symEncryptedKey = (ASN1Sequence) root.getObjectAt(1);
            ASN1Integer integer = (ASN1Integer) symEncryptedKey.getObjectAt(0);
            ASN1Integer integer1 = (ASN1Integer) symEncryptedKey.getObjectAt(1);
            //导出创建密钥服务时生成的非对称密钥公钥解密数字信封，获取对称密钥。
            //获取非对称密钥索引
            Long keyIndex = secretKeyManage.getSecretKeyIndex();
            //解密
            byte[] C1 = ((DEROctetString) symEncryptedKey.getObjectAt(2)).getOctets();
            byte[] C2 = ((DEROctetString) symEncryptedKey.getObjectAt(3)).getOctets();
            byte[] seq = BitOperator.concatAll(integer.getValue().toByteArray(),integer1.getValue().toByteArray(),C1,C2);
            byte[] privKeyEnc = mgr.ZaykDecrypt_ECC(keyIndex.intValue(),1,seq);
            symmKey= Base64.toBase64String(privKeyEnc);
        }
        return symmKey;
    }

    public static String getCacheNameDefaultSym(String keyType, int keyId) {
        return keyType + ":" + keyId;
    }
    /**
     * 使用系统主密钥解密
     * @param
     ** @return
     * @throws ZAYKException
     */
    private String  SMKDecryptMethod(String plaintext) throws ZAYKException {
        byte[] ciphertext = Base64.decode(plaintext);

        if (ObjectUtil.isEmpty(mgr)) {
            mgr = new ZaykManageClass();
            mgr.Initialize(crytoCartTypeSInt);
        }
        byte[] pucOutData = new byte[ciphertext.length];
        int ret = mgr.SMKDecrypt(ZaSDSUtils.SGD_SM4_ECB, ciphertext, pucOutData);
        if (ret != 0) {
            if(ret==SDR_SYS_MASTER_KEY_NO_EXIST) {
                log.debug("Warning:请检查是否已生成系统主密钥！");
            }
            throw new ZAYKException("ERROR:SM4 decryption failed！error code=" + ret);
        }
        byte[] unpad = PaddUtils.unpad(pucOutData, 16);
        return new String(unpad);
    }
    //获取实例信息
    private DbhsmDbInstance getDbInstanceInfo(Long instanceId) throws ZAYKException {
        DbhsmDbInstance dbInstanceQueryParam = new DbhsmDbInstance();

        DbhsmDbInstance instances = dbInstanceService.selectDbhsmDbInstanceById(instanceId);
        if (!Optional.ofNullable(instances).isPresent()) {
            log.info("数据库实例：" + dbInstanceQueryParam + "不存在");
            throw new ZAYKException("数据库实例不存在" + dbInstanceQueryParam, -1);
        }
        return instances;
    }

    //获取加密列信息
    private DbhsmEncryptColumns getEncryptColumnsInfo(RequestApiData requestApiData) throws ZAYKException {
        //查询加密列对象
        DbhsmEncryptColumns dbEncryptColumns = encryptColumnsService.selectDbhsmEncryptColumnsById(requestApiData.getPid());
        if (!Optional.ofNullable(dbEncryptColumns).isPresent()) {
            throw new ZAYKException("未查询到PID为：" + requestApiData.getPid() + "的加密列");
        }
        return dbEncryptColumns;
    }

    //组装查询参数
    private RequestApiData getRequestApiData(byte[] requestApiDataByte) throws IOException, ZAYKException {
        // pid           --策略唯一标识
        // dbtype        --数据库类型
        // dbversion     --数据库版本
        // instancename  --实例名
        // dbname        --数据库名
        // tablename     --表名
        // columnname    --列名
        // username      --用户名
        ASN1Sequence requestApiDataASN1 = (ASN1Sequence) ASN1Sequence.fromByteArray(requestApiDataByte);
        // pid           --策略唯一标识
        // ASN1Integer pid = (ASN1Integer) requestApiDataASN1.getObjectAt(0);
        DEROctetString pid = (DEROctetString) requestApiDataASN1.getObjectAt(0);
        // dbtype        --数据库类型
        ASN1Integer dbtype = (ASN1Integer) requestApiDataASN1.getObjectAt(1);
        // dbversion     --数据库版本
        DEROctetString dbversion = (DEROctetString) requestApiDataASN1.getObjectAt(2);
        // instancename  --实例名
        DEROctetString instancename = (DEROctetString) requestApiDataASN1.getObjectAt(3);
        // dbname        --数据库名
        DEROctetString dbname = (DEROctetString) requestApiDataASN1.getObjectAt(4);
        // tablename     --表名
        DEROctetString tablename = (DEROctetString) requestApiDataASN1.getObjectAt(5);
        // columnname    --列名
        DEROctetString columnname = (DEROctetString) requestApiDataASN1.getObjectAt(6);
        // username      --用户名
        DEROctetString username = (DEROctetString) requestApiDataASN1.getObjectAt(7);


        RequestApiData requestApiData = new RequestApiData();
        if ("".equals(new String(pid.getOctets()))) {
            log.info("策略唯一标识pid为空");
            throw new ZAYKException("策略唯一标识pid为空", -1);
        }
        if ("".equals(new String(instancename.getOctets()))) {
            log.info("实例名为空");
            throw new ZAYKException("实例名为空", -1);
        }
        if ("".equals(new String(dbname.getOctets()))) {
            log.info("数据库名为空");
            throw new ZAYKException("数据库名为空", -1);
        }
        if ("".equals(new String(tablename.getOctets()))) {
            log.info("表名为空");
            throw new ZAYKException("表名为空", -1);
        }
        if ("".equals(new String(columnname.getOctets()))) {
            log.info("列名为空");
            throw new ZAYKException("列名为空", -1);
        }
        if ("".equals(new String(username.getOctets()))) {
            log.info("用户名为空");
            throw new ZAYKException("用户名为空", -1);
        }
        requestApiData.setPid(new String(pid.getOctets()));
        requestApiData.setDatabaseType(dbtype.getValue().intValue());
        requestApiData.setDatabaseEdition(new String(dbversion.getOctets()));
        requestApiData.setInstanceName(new String(instancename.getOctets()));
        requestApiData.setDatabaseServerName(new String(dbname.getOctets()));
        requestApiData.setDbTableName(new String(tablename.getOctets()));
        requestApiData.setEncryptColumns(new String(columnname.getOctets()));
        requestApiData.setUserName(new String(username.getOctets()));
        log.info("请求参数：" + requestApiData);
        return requestApiData;
    }

    private String getKMIPKey(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception {
       int ret = secretKeyManageService.updateDbhsmSecretKeyManage(dbhsmSecretKeyManage);
        if (ret == 0){
            throw new Exception("从KMIP服务同步密钥失败");
        }
        List<DbhsmSecretKeyManage> dbhsmSecretKeyManages = secretKeyManageService.selectDbhsmSecretKeyManageList(dbhsmSecretKeyManage);
        if (StringUtils.isEmpty(dbhsmSecretKeyManages)){
            throw new Exception("获取密钥失败");
        }
        DbhsmSecretKeyManage secretKeyManage = new DbhsmSecretKeyManage();
        return secretKeyManage.getSecretKey();
    }
}
