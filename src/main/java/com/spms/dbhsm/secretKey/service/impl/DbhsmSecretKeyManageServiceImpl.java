package com.spms.dbhsm.secretKey.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.ccsp.cert.sanwei.ByteUtils;
import com.ccsp.cert.sm4.SM4Utils;
import com.ccsp.common.core.domain.R;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.PaddUtils;
import com.ccsp.common.core.utils.SpringUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.security.utils.SecurityUtils;
import com.ccsp.system.api.hsmSvsTsaApi.RemoteSecretKeyService;
import com.ccsp.system.api.hsmSvsTsaApi.SpmsDevBaseDataService;
import com.ccsp.system.api.hsmSvsTsaApi.domain.HsmSm2SecretKey;
import com.ccsp.system.api.hsmSvsTsaApi.domain.HsmSymmetricSecretKey;
import com.sc.kmip.client.KMSClientInterface;
import com.sc.kmip.container.KMIPContainer;
import com.sc.kmip.kmipenum.EnumCryptographicAlgorithm;
import com.spms.common.HttpClientUtil;
import com.spms.common.JSONDataUtil;
import com.spms.common.KeyPairInfo;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.RandomPasswordGenerator;
import com.spms.common.kmip.KmipServicePoolFactory;
import com.spms.common.kmip.ZaKmipUtil;
import com.spms.common.sm2.SM2Utils;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import com.spms.dbhsm.secretKey.domain.DbhsmSecretKeyManage;
import com.spms.dbhsm.secretKey.domain.KeyResponse;
import com.spms.dbhsm.secretKey.mapper.DbhsmSecretKeyManageMapper;
import com.spms.dbhsm.secretKey.service.IDbhsmSecretKeyManageService;
import com.spms.dbhsm.secretService.domain.DbhsmSecretService;
import com.spms.dbhsm.secretService.mapper.DbhsmSecretServiceMapper;
import com.zayk.ciphercard.ZaykManageClass;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * 数据库密钥Service业务层处理
 *
 * @author ccsp
 * @date 2023-09-22
 */
@Slf4j
@Service
public class DbhsmSecretKeyManageServiceImpl implements IDbhsmSecretKeyManageService
{
    @Autowired
    private DbhsmSecretKeyManageMapper dbhsmSecretKeyManageMapper;
    @Autowired
    private DbhsmSecretServiceMapper dbhsmSecretServiceMapper;

    @Autowired
    DbhsmEncryptColumnsMapper dbhsmEncryptColumnsMapper;
    @Autowired
    private RemoteSecretKeyService remoteSecretKeyService;

    @Autowired
    public static SpmsDevBaseDataService devBaseDataService;

    /**
     * 查询数据库密钥
     *
     * @param id 数据库密钥主键
     * @return 数据库密钥
     */
    @Override
    public DbhsmSecretKeyManage selectDbhsmSecretKeyManageById(Long id)
    {
        return dbhsmSecretKeyManageMapper.selectDbhsmSecretKeyManageById(id);
    }

    /**
     * 查询数据库密钥列表
     *
     * @param dbhsmSecretKeyManage 数据库密钥
     * @return 数据库密钥
     */
    @Override
    public List<DbhsmSecretKeyManage> selectDbhsmSecretKeyManageList(DbhsmSecretKeyManage dbhsmSecretKeyManage)
    {
        return dbhsmSecretKeyManageMapper.selectDbhsmSecretKeyManageList(dbhsmSecretKeyManage);
    }

    /**
     * 新增数据库密钥
     *
     * @param dbhsmSecretKeyManage 数据库密钥
     * @return 结果
     */
    @SuppressWarnings("Duplicates")
    @Override
    public int insertDbhsmSecretKeyManage(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception {

        //密钥类型为对称密钥
        if (dbhsmSecretKeyManage.getSecretKeyType().equals(DbConstants.SECRET_KEY_TYPE_SYM)) {
            dbhsmSecretKeyManage.setSecretKeyId("Symm" + UUID.randomUUID().toString().substring(0, 14) + dbhsmSecretKeyManage.getSecretKeyIndex());
        } else {
            dbhsmSecretKeyManage.setSecretKeyId("Asymm" + UUID.randomUUID().toString().substring(0, 14) + dbhsmSecretKeyManage.getSecretKeyIndex());
        }

        //如果密钥来源为卡内密钥，切该索引密钥未生成，则生成卡内密钥，如果为KMIP则只保存密钥关系
        if(DbConstants.KEY_SOURCE_SECRET_CARD.intValue() == dbhsmSecretKeyManage.getSecretKeySource().intValue()) {
            int keyGenerationMode = JSONDataUtil.getSecretKeyGenerateType(DbConstants.SYSDATA_ALGORITHM_TYPE_SYK);
            if (keyGenerationMode == DbConstants.HARD_SECRET_KEY) {
                generatorSym(dbhsmSecretKeyManage.getSecretKeyType(), dbhsmSecretKeyManage.getSecretKeyIndex().intValue(), dbhsmSecretKeyManage.getSecretKeyLength());
            } else if (keyGenerationMode == DbConstants.BULK_SECRET_KEY) {
                generatorBulkKey(dbhsmSecretKeyManage);
            }
        }else if(DbConstants.KEY_SOURCE_KMIP.intValue() == dbhsmSecretKeyManage.getSecretKeySource().intValue()) {
            //调用KMIP
            KMSClientInterface kmipService = KmipServicePoolFactory.getKmipServicePool(dbhsmSecretKeyManage.getSecretKeyServer());
            DbhsmSecretService service = null;
            service = dbhsmSecretServiceMapper.selectDbhsmSecretServiceBySecretService(dbhsmSecretKeyManage.getSecretKeyServer());
            if (kmipService == null){
                log.error("从池中获取名称为{}的KMIP服务异常",dbhsmSecretKeyManage.getSecretKeyServer());
                KmipServicePoolFactory.bind(service);
            }

            //在进行获取一次连接
            kmipService = KmipServicePoolFactory.getKmipServicePool(dbhsmSecretKeyManage.getSecretKeyServer());
            if (kmipService == null){
                log.error("创建名称为" + dbhsmSecretKeyManage.getSecretKeyServer() + "KMIP连接异常:");
                throw new Exception("创建名称为" + dbhsmSecretKeyManage.getSecretKeyServer() + "KMIP连接异常");
            }


            //先获取一次密钥
            KMIPContainer kmipResponse = ZaKmipUtil.wrapKey(kmipService,dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", "SM2",service.getUserName(),service.getPassword());
            if (kmipResponse == null) {
                log.error("首次获取KMIP失败,kmipResponse == null");
                throw new Exception("获取KMIP失败");
            }

            if (!DbConstants.SUCCESS_STR.equalsIgnoreCase(kmipResponse.getResultStatus())) {
                //KMIP不存在的情况下，先生成再获取
                KMIPContainer kmipContainer = ZaKmipUtil.createSymmetricKey(kmipService,EnumCryptographicAlgorithm.SM4, 128, dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "");
                if (kmipContainer == null) {
                    log.error("生成KMIP失败,kmipContainer == null");
                    throw new Exception("生成KMIP失败");
                }

                if (!DbConstants.SUCCESS_STR.equalsIgnoreCase(kmipContainer.getResultStatus())) {
                    log.error("生成KMIP失败,status:" + kmipContainer.getResultStatus());
                    throw new Exception("生成KMIP失败,status:" + kmipContainer.getResultStatus());
                }

                kmipResponse = ZaKmipUtil.wrapKey(kmipService,dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", "SM2",service.getUserName(),service.getPassword());
                if (kmipResponse == null) {
                    log.error("再次获取KMIP失败,kmipResponse == null");
                    throw new Exception("获取KMIP失败");
                }

                if (!DbConstants.SUCCESS_STR.equalsIgnoreCase(kmipResponse.getResultStatus())) {
                    log.error("再次获取KMIP失败,status ：" + kmipResponse.getResultStatus());
                    throw new Exception("获取KMIP失败,status:" + kmipResponse.getResultStatus());
                }
            }

            //获取数字信封
            String keyMaterial = kmipResponse.getKeyMaterial();
            if(StringUtils.isEmpty(keyMaterial)){
                log.error("获取密钥失败,返回为null" );
                throw new Exception("获取KMIP失败,status:" + kmipResponse.getResultStatus());
            }
            log.info("keyMaterial:" + keyMaterial);
            dbhsmSecretKeyManage.setSecretKey(keyMaterial);
        }else if(DbConstants.KEY_SOURCE_JIT.intValue() == dbhsmSecretKeyManage.getSecretKeySource().intValue()){
            generatorSoftKey(dbhsmSecretKeyManage);
        }else {
            log.error("密钥来源参数错误:" + dbhsmSecretKeyManage.getSecretKeySource().intValue());
            throw new Exception("密钥来源参数错误:" + dbhsmSecretKeyManage.getSecretKeySource().intValue());
        }
        dbhsmSecretKeyManage.setCreateTime(DateUtils.getNowDate());
        dbhsmSecretKeyManage.setCreateBy(SecurityUtils.getUsername());
        return dbhsmSecretKeyManageMapper.insertDbhsmSecretKeyManage(dbhsmSecretKeyManage);
    }

    private void generatorBulkKey(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws ZAYKException {
        int generatorKeyPair = -1;
        int secretKeyLength=dbhsmSecretKeyManage.getSecretKeyLength();
        Long keyIndex = dbhsmSecretKeyManage.getSecretKeyIndex();
        Integer secretKeyType = dbhsmSecretKeyManage.getSecretKeyType();
        Integer crytoCartTypeSInt = null;

        //获取卡类型
        Object crytoCartTypeObj = JSONDataUtil.getSysDataToDB(DbConstants.cryptoCardType);
        if (null != crytoCartTypeObj) {
            crytoCartTypeSInt = Integer.parseInt(crytoCartTypeObj.toString());
        }
        // 调用jna接口
        ZaykManageClass mgr = new ZaykManageClass();
        if(ObjectUtil.isEmpty(crytoCartTypeObj)){
            throw new ZAYKException("获取卡类型失败，请检查数据库配置");
        }
        mgr.Initialize(crytoCartTypeSInt);
        KeyPairInfo keyPairInfo = new KeyPairInfo();

        // 大容量密钥--对称
        byte[] random = new byte[secretKeyLength];
        generatorKeyPair = mgr.GenerateRandom(secretKeyLength, random);
        keyPairInfo.setPrivateKey(Base64.toBase64String(random));
        keyPairInfo.setPublicKey(Base64.toBase64String(random));
        keyPairInfo.setSecretKeyIndex(Math.toIntExact(keyIndex));
        keyPairInfo.setSecretKeyUsage(1);
        keyPairInfo.setSecretKeyModuleLength(secretKeyLength);
        keyPairInfo.setSecretKeyType(secretKeyType.toString());
        mgr.finalize();
        //插入数据库
        ArrayList<HsmSymmetricSecretKey> secretKeyArrayList = new ArrayList<>();
        HsmSymmetricSecretKey symmetricSecretKey = new HsmSymmetricSecretKey();
        symmetricSecretKey.setSecretKeyId(UUID.randomUUID().toString());
        symmetricSecretKey.setSecretKeyIndex(String.valueOf(keyPairInfo.getSecretKeyIndex()));
        symmetricSecretKey.setSecretKeyModuleLength(secretKeyLength);
        symmetricSecretKey.setSecretKeyUsage(keyPairInfo.getSecretKeyUsage());
        symmetricSecretKey.setPublicKey(keyPairInfo.getPublicKey());
        symmetricSecretKey.setPrivateKey(keyPairInfo.getPrivateKey());
        symmetricSecretKey.setCreateTime(DateUtils.getNowDate());
        secretKeyArrayList.add(symmetricSecretKey);
        R<AjaxResult2> batchR = remoteSecretKeyService.insertHsmSymSecretKeyBatch(secretKeyArrayList);
        if (batchR.getCode() != DbConstants.SUCCESS) {
            log.error(" insertHsmSymSecretKeyBatch error :" + batchR.getMsg());
        }
        return;
    }

    /**
     * 纯软密钥
     * @param dbhsmSecretKeyManage
     * @throws ZAYKException
     */
    private void generatorSoftKey(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception {
        int secretKeyLength=dbhsmSecretKeyManage.getSecretKeyLength();
        Long keyIndex = dbhsmSecretKeyManage.getSecretKeyIndex();
        Integer secretKeyType = dbhsmSecretKeyManage.getSecretKeyType();

        KeyPairInfo keyPairInfo = new KeyPairInfo();

        // 软密钥--对称
        //byte[] random = RandomUtil.randomBytes(secretKeyLength);
        String random = RandomPasswordGenerator.generateRandomPassword(secretKeyLength);
        log.info("Soft Random:{}\n" , random);
        keyPairInfo.setPrivateKey(Base64.toBase64String(random.getBytes()));
        keyPairInfo.setPublicKey(Base64.toBase64String(random.getBytes()));
        keyPairInfo.setSecretKeyIndex(Math.toIntExact(keyIndex));
        keyPairInfo.setSecretKeyUsage(1);
        keyPairInfo.setSecretKeyModuleLength(secretKeyLength);
        keyPairInfo.setSecretKeyType(secretKeyType.toString());
        //插入数据库
        ArrayList<HsmSymmetricSecretKey> secretKeyArrayList = new ArrayList<>();
        HsmSymmetricSecretKey symmetricSecretKey = new HsmSymmetricSecretKey();
        symmetricSecretKey.setSecretKeyId(UUID.randomUUID().toString());
        symmetricSecretKey.setSecretKeyIndex(String.valueOf(keyPairInfo.getSecretKeyIndex()));
        symmetricSecretKey.setSecretKeyModuleLength(secretKeyLength);
        symmetricSecretKey.setSecretKeyUsage(keyPairInfo.getSecretKeyUsage());
        symmetricSecretKey.setPublicKey(keyPairInfo.getPublicKey());
        symmetricSecretKey.setPrivateKey(keyPairInfo.getPrivateKey());
        symmetricSecretKey.setCreateTime(DateUtils.getNowDate());
        secretKeyArrayList.add(symmetricSecretKey);

        //使用外部接口生成系统主密钥
        String systemMasterKey = JSONDataUtil.getSysDataToDB(DbConstants.DBENC_SYSTEM_MASTER_KEY);
        if(StringUtils.isEmpty(systemMasterKey)){
            //使用外部接口生成系统主密钥
             systemMasterKey = getSystemMasterKeyJIT(dbhsmSecretKeyManage);
        }
        //系统主密钥加密软密钥
        byte[] plaintext = random.getBytes();
        //加密
        byte[] ciphertext = new byte[0];
        //数据库配置的系统主密钥存在则使用配置的系统主密钥加密
        ciphertext = SM4Utils.encryptData_ECB(plaintext, ByteUtils.hexToBytes(systemMasterKey));
        dbhsmSecretKeyManage.setSecretKey(Base64.toBase64String(ciphertext));
    }

    private String getSystemMasterKeyJIT(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception {
        //从密码密码服务中获取sm2密钥索引
        DbhsmSecretService dbhsmSecretService = new DbhsmSecretService();
        dbhsmSecretService.setSecretService(dbhsmSecretKeyManage.getSecretKeyServer());
        List<DbhsmSecretService> serviceList = dbhsmSecretServiceMapper.selectDbhsmSecretServiceList(dbhsmSecretService);
        if(CollectionUtils.isEmpty(serviceList)||ObjectUtil.isEmpty(serviceList.get(0))){
            throw new ZAYKException("密码服务不存在！");
        }
        String authorization = JSONDataUtil.getSysDataToDB(DbConstants.AUTHORIZATION);
        if (authorization == null){
            throw new ZAYKException("认证信息Authorization为空！，请检查配置！");
        }
        DbhsmSecretService hsmSecretService = serviceList.get(0);
        byte[] encode = Base64.encode(authorization.getBytes(StandardCharsets.UTF_8));
        authorization = "Basic "+new String(encode);
        String keyIdentifier = getKeyIdentifier(hsmSecretService,authorization);
        log.info("获取密钥的标识："+keyIdentifier);
        return getSystemMasterKey(keyIdentifier,hsmSecretService,authorization);

    }

    /**根据密钥标识获取KEK密钥
     * 1、获取sm2私钥密文
     * 2、获取系统主密钥，使用系统主密钥解密sm2私钥密文得到sm2私钥
     * 3、调用吉大接口获取kek密钥数字信封（接口传sm2公钥）
     * 4、使用sm2私钥解密kek密钥数字信封得到kek密钥作为数据库加密网关的系统主密钥，用于加密软密钥，（达梦配置加密列时使用到该软密钥）
     *
     * @param keyIdentifier
     * @param hsmSecretService
     * @param authorization
     * @return
     * @throws ZAYKException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchProviderException
     */
    private String getSystemMasterKey(String keyIdentifier,DbhsmSecretService hsmSecretService,String authorization) throws Exception {
        String systemMasterKey = null;
        String dbencSystemMasterKey = null;
        int secretKeyindex = Math.toIntExact(hsmSecretService.getSecretKeyIndex());
        //根据密钥索引获取sm2密钥
        R<HsmSm2SecretKey> hsmSm2SecretKeyR = remoteSecretKeyService.selectSm2SecretKeyInfo(secretKeyindex, DbConstants.SECRET_KEY_USAGE_ENC);
        HsmSm2SecretKey sm2SecretKey = hsmSm2SecretKeyR.getData();
        if (sm2SecretKey == null) {
            throw new ZAYKException("获取SM2加密密钥异常");
        }
        String ciphertext =sm2SecretKey.getPrivateKey();
        byte[] decode = Base64.decode(ciphertext);
        //获取系统主密钥
        Object systemMasterKeyObj = JSONDataUtil.getSysDataToDB(DbConstants.SYSTEM_MASTER_KEY);
        if (ObjectUtil.isNotEmpty(systemMasterKeyObj)) {
            systemMasterKey = systemMasterKeyObj.toString();
            log.info("已配置系统主密钥");
        }else {
            log.info("未配置系统主密钥");
            throw new ZAYKException("未配置系统主密钥systemMasterKey");
        }
        //数据库配置的系统主密钥存在则使用配置的系统主密钥加密
        byte[] plaintext = SM4Utils.decryptData_ECB(decode,ByteUtils.hexToBytes(systemMasterKey));
        byte[] unpad = PaddUtils.unpad(plaintext, 16);
        String privateKey = new String(unpad);
        String ip = hsmSecretService.getServiceIp();
        String port = hsmSecretService.getServicePort();
        String url = "https://"+ip+":"+port+"/api/v1/kms/kek/getCurrentKey";
        Map<String,Object> map = new HashMap<>();
        map.put("keyCode",keyIdentifier);
        map.put("wrapMethod",DbConstants.JIT_ENCRYPTED_TYPE_PUBLICKEY);
        map.put("wrapData",sm2SecretKey.getPublicKey());
        map.put("algorithm",DbConstants.JIT_ALGORITHM_TYPE_SM2);
        log.info("调用获取最新版的KEK密钥的接口：ur：{}，参数：{}",url,map);
        KeyResponse keyResponse = JSON.parseObject(HttpClientUtil.sendPostJson(url, map,authorization), KeyResponse.class);
        String symmetricKey = keyResponse.getData().getSymmetricKey();
        log.info("调用获取最新版的KEK密钥的接口返回结果：{}",keyResponse);
        byte[] bytes = SM2Utils.decrypt_jit(privateKey, symmetricKey);
        dbencSystemMasterKey= Hex.toHexString(bytes);
        log.info("解密后的KEK：{}",dbencSystemMasterKey);
        //主密钥存数据库
        JSONDataUtil.setSysDataToDB(DbConstants.DBENC_SYSTEM_MASTER_KEY,dbencSystemMasterKey);
        if(devBaseDataService==null) {
            devBaseDataService = SpringUtils.getBean(SpmsDevBaseDataService.class);
        }
        devBaseDataService.updateSysBaseDataByKey("dbencSystemMasterKey",dbencSystemMasterKey);
        return dbencSystemMasterKey;
    }

    /**获取密钥标识*/
    private  String  getKeyIdentifier(DbhsmSecretService hsmSecretService,String authorization) throws ZAYKException {

        Map<String,Object> map = new HashMap<>();
        String ip = hsmSecretService.getServiceIp();
        String port = hsmSecretService.getServicePort();
        String url = "https://"+ip+":"+port+"/api/v1/kms/kek/createKey";
        map.put("algorithm","SM4");
        map.put("length",128);
        log.info("获取密钥标识：url: {},参数: {}",url,map);
        KeyResponse keyResponse = JSON.parseObject(HttpClientUtil.sendPostJson(url, map,authorization), KeyResponse.class);
        if(ObjectUtil.isNotEmpty(keyResponse)&&!keyResponse.getCode().equals(DbConstants.JIT_SUCCESS_CODE)){
            log.info("获取JIT密钥标识失败！");
            throw new ZAYKException("获取密钥标识失败！");
        }
        String keyIdentifier = keyResponse.getData().getKeyCode();
        if(StringUtils.isEmpty(keyIdentifier)){
            log.info("返回的密钥标识为空！");
            throw new ZAYKException("返回的密钥标识为空！");
        }
        return keyIdentifier;
    }

    /**
     * 修改数据库密钥
     *
     * @param dbhsmSecretKeyManage 数据库密钥
     * @return 结果
     */
    @SuppressWarnings("Duplicates")
    @Override
    public int updateDbhsmSecretKeyManage(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception {
        //如果密钥来源为卡内密钥，切该索引密钥未生成，则生成卡内密钥，如果为KMIP则只保存密钥关系
        if(DbConstants.KEY_SOURCE_SECRET_CARD.intValue() == dbhsmSecretKeyManage.getSecretKeySource().intValue()) {
            int keyGenerationMode = JSONDataUtil.getSecretKeyGenerateType(DbConstants.SYSDATA_ALGORITHM_TYPE_SYK);
            if (keyGenerationMode == DbConstants.HARD_SECRET_KEY) {
                generatorSym(dbhsmSecretKeyManage.getSecretKeyType(), dbhsmSecretKeyManage.getSecretKeyIndex().intValue(), dbhsmSecretKeyManage.getSecretKeyLength());
            } else if (keyGenerationMode == DbConstants.BULK_SECRET_KEY) {
                generatorBulkKey(dbhsmSecretKeyManage);
            } else{
                generatorSoftKey(dbhsmSecretKeyManage);
            }
        }else if(DbConstants.KEY_SOURCE_KMIP.intValue() == dbhsmSecretKeyManage.getSecretKeySource().intValue()) {
            //调用KMIP
            KMSClientInterface kmipService = KmipServicePoolFactory.getKmipServicePool(dbhsmSecretKeyManage.getSecretKeyServer());
            DbhsmSecretService service = null;
            service = dbhsmSecretServiceMapper.selectDbhsmSecretServiceBySecretService(dbhsmSecretKeyManage.getSecretKeyServer());
            if (kmipService == null){
                log.error("从池中获取名称为{}的KMIP服务异常",dbhsmSecretKeyManage.getSecretKeyServer());
                KmipServicePoolFactory.bind(service);
            }

            //在进行获取一次连接
            kmipService = KmipServicePoolFactory.getKmipServicePool(dbhsmSecretKeyManage.getSecretKeyServer());
            if (kmipService == null){
                log.error("创建名称为" + dbhsmSecretKeyManage.getSecretKeyServer() + "KMIP连接异常:");
                throw new Exception("创建名称为" + dbhsmSecretKeyManage.getSecretKeyServer() + "KMIP连接异常");
            }


            //先获取一次密钥
            KMIPContainer kmipResponse = ZaKmipUtil.wrapKey(kmipService,dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", "SM2",service.getUserName(),service.getPassword());
            if (kmipResponse == null) {
                log.error("首次获取KMIP失败,kmipResponse == null");
                throw new Exception("获取KMIP失败");
            }

            if (!DbConstants.SUCCESS_STR.equalsIgnoreCase(kmipResponse.getResultStatus())) {
                //KMIP不存在的情况下，先生成再获取
                KMIPContainer kmipContainer = ZaKmipUtil.createSymmetricKey(kmipService,EnumCryptographicAlgorithm.SM4, 128, dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "");
                if (kmipContainer == null) {
                    log.error("生成KMIP失败,kmipContainer == null");
                    throw new Exception("生成KMIP失败");
                }

                if (!DbConstants.SUCCESS_STR.equalsIgnoreCase(kmipContainer.getResultStatus())) {
                    log.error("生成KMIP失败,status:" + kmipContainer.getResultStatus());
                    throw new Exception("生成KMIP失败,status:" + kmipContainer.getResultStatus());
                }

                kmipResponse = ZaKmipUtil.wrapKey(kmipService,dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", dbhsmSecretKeyManage.getSecretKeyIndex().intValue() + "", "SM2",service.getUserName(),service.getPassword());
                if (kmipResponse == null) {
                    log.error("再次获取KMIP失败,kmipResponse == null");
                    throw new Exception("获取KMIP失败");
                }

                if (!DbConstants.SUCCESS_STR.equalsIgnoreCase(kmipResponse.getResultStatus())) {
                    log.error("再次获取KMIP失败,status ：" + kmipResponse.getResultStatus());
                    throw new Exception("获取KMIP失败,status:" + kmipResponse.getResultStatus());
                }
            }
            String keyMaterial = kmipResponse.getKeyMaterial();
            if(StringUtils.isEmpty(keyMaterial)){
                log.error("获取密钥失败,返回为null" );
                throw new Exception("获取KMIP失败,status:" + kmipResponse.getResultStatus());
            }
            //获取数字信封
            log.info("keyMaterial:" + keyMaterial);
            dbhsmSecretKeyManage.setSecretKey(keyMaterial);
        }else {
            log.error("密钥来源参数错误:" + dbhsmSecretKeyManage.getSecretKeySource().intValue());
            throw new Exception("密钥来源参数错误:" + dbhsmSecretKeyManage.getSecretKeySource().intValue());
        }
        dbhsmSecretKeyManage.setUpdateTime(DateUtils.getNowDate());
        return dbhsmSecretKeyManageMapper.updateDbhsmSecretKeyManage(dbhsmSecretKeyManage);
    }

    /**
     * 批量删除数据库密钥
     *
     * @param ids 需要删除的数据库密钥主键
     * @return 结果
     */
    @Override
    public AjaxResult2 deleteDbhsmSecretKeyManageByIds(Long[] ids) throws ZAYKException {
        int i = 0;
        List<String> secretKeyIds = new ArrayList<>();
        for (Long id : ids) {
            DbhsmSecretKeyManage secretKeyManage = selectDbhsmSecretKeyManageById(id);
            //判断是否已被加密列使用
            DbhsmEncryptColumns dbhsmEncryptColumns = new DbhsmEncryptColumns();
            dbhsmEncryptColumns.setSecretKeyId(secretKeyManage.getSecretKeyId());
            List<DbhsmEncryptColumns> list = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(dbhsmEncryptColumns);
            if(list.size() > 0){
                secretKeyIds.add(secretKeyManage.getSecretKeyId());
                continue;
            }
            i = deleteDbhsmSecretKeyManageById(id);
            //根据索引删除对称密钥
            int keyGenerationMode = JSONDataUtil.getSecretKeyGenerateType(DbConstants.SYSDATA_ALGORITHM_TYPE_SYK);
            if (keyGenerationMode == DbConstants.BULK_SECRET_KEY) {
                remoteSecretKeyService.removeSymKey(String.valueOf(secretKeyManage.getSecretKeyIndex()));
            }
        }
        return secretKeyIds.size() > 0 ? AjaxResult2.error("密钥ID为:" + String.join(",", secretKeyIds) + "的密钥已被加密列使用,请先删除加密列") : AjaxResult2.success();
        //return dbhsmSecretKeyManageMapper.deleteDbhsmSecretKeyManageByIds(ids);
    }

    /**
     * 删除数据库密钥信息
     *
     * @param id 数据库密钥主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmSecretKeyManageById(Long id)
    {

        return dbhsmSecretKeyManageMapper.deleteDbhsmSecretKeyManageById(id);
    }

    /**
     * 校验密钥名称是否唯一
     * @param dbhsmSecretKeyManage
     * @return
     */
    @Override
    public String checkSecretKeyUnique(DbhsmSecretKeyManage dbhsmSecretKeyManage) {
        DbhsmSecretKeyManage secretKeyUnique = dbhsmSecretKeyManageMapper.checkSecretKeyUnique(dbhsmSecretKeyManage.getSecretKeyName());
        if (secretKeyUnique == null) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        if (StringUtils.isEmpty(secretKeyUnique.getSecretKeyName())){
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }

        //有值的情况，如果是修改，允许有一条
        if (dbhsmSecretKeyManage.getId() != null && dbhsmSecretKeyManage.getId() == secretKeyUnique.getId().intValue()) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
    }
    /**
     * 校验密钥名称是否唯一
     * @param dbhsmSecretKeyManage
     * @return
     */
    @Override
    public String checkSecretKeyUniqueEdit(DbhsmSecretKeyManage dbhsmSecretKeyManage) {
        DbhsmSecretKeyManage secretKeyUnique = dbhsmSecretKeyManageMapper.checkSecretKeyUnique(dbhsmSecretKeyManage.getSecretKeyName());
        if (ObjectUtil.isEmpty(secretKeyUnique)) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        //有值的情况，如果是修改，允许有一条
        if (dbhsmSecretKeyManage.getId() != null && dbhsmSecretKeyManage.getId() == secretKeyUnique.getId().intValue()) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
    }

    @Override
    public String checkSecretKeyIndexUnique(DbhsmSecretKeyManage dbhsmSecretKeyManage) {
        DbhsmSecretKeyManage secretKeyManage = new DbhsmSecretKeyManage();
        secretKeyManage.setSecretKeyIndex(dbhsmSecretKeyManage.getSecretKeyIndex());
        secretKeyManage.setSecretKeySource(dbhsmSecretKeyManage.getSecretKeySource());
        List<DbhsmSecretKeyManage> secretKeyUniqueList = dbhsmSecretKeyManageMapper.selectDbhsmSecretKeyManageList(secretKeyManage);
        if (StringUtils.isEmpty(secretKeyUniqueList)) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }

        if (secretKeyUniqueList.size() > 1) {
            //至少两条
            return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
        }

        //只有一条
        if (dbhsmSecretKeyManage.getId() != null && dbhsmSecretKeyManage.getId().intValue() == secretKeyUniqueList.get(0).getId().intValue()) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
    }
    @Override
    public String checkSecretKeyIndexUniqueEdit(DbhsmSecretKeyManage dbhsmSecretKeyManage) {
        DbhsmSecretKeyManage secretKeyManage = new DbhsmSecretKeyManage();
        secretKeyManage.setSecretKeyIndex(dbhsmSecretKeyManage.getSecretKeyIndex());
        secretKeyManage.setSecretKeySource(dbhsmSecretKeyManage.getSecretKeySource());
        List<DbhsmSecretKeyManage> secretKeyUniqueList = dbhsmSecretKeyManageMapper.selectDbhsmSecretKeyManageList(secretKeyManage);
        if (StringUtils.isEmpty(secretKeyUniqueList)) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }

        //只有一条
        if (dbhsmSecretKeyManage.getId() != null && dbhsmSecretKeyManage.getId().intValue() == secretKeyUniqueList.get(0).getId().intValue()) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
    }

    private void generatorSym(Integer keyType, Integer keyIndex, Integer keyLength) throws Exception {
        Integer crytoCartTypeSInt = null;
        Object crytoCartTypeObj = JSONDataUtil.getSysDataToDB(DbConstants.cryptoCardType);
        if (null != crytoCartTypeObj) {
            crytoCartTypeSInt = Integer.parseInt(crytoCartTypeObj.toString());
        }
        // 调用jna接口
        ZaykManageClass mgr = new ZaykManageClass();
        mgr.Initialize(crytoCartTypeSInt);
        Integer GeneratorKeyPair = -1;
        if (DbConstants.HSM_CARD_ZASEC_SC10 == crytoCartTypeSInt || DbConstants.HSM_CARD_ZASEC_SC11 == crytoCartTypeSInt || DbConstants.HSM_CARD_ZASEC_SC68 == crytoCartTypeSInt
                || DbConstants.HSM_CARD_ZASEC_SC12 == crytoCartTypeSInt || DbConstants.HSM_CARD_ZASEC_SC12V == crytoCartTypeSInt || DbConstants.HSM_CARD_ZASEC_SC20 == crytoCartTypeSInt || DbConstants.HSM_CARD_ZASEC_SC30 == crytoCartTypeSInt) {
            GeneratorKeyPair = mgr.GeneratorKeyPair_SYM(keyIndex, Integer.valueOf(keyLength));
        } else {
            GeneratorKeyPair = mgr.GeneratorKeyPair(keyType, keyLength, keyIndex);
        }
        if (GeneratorKeyPair != 0) {
            log.error("addSymmetryManagement ===============》 添加" + keyIndex + "号对称密钥失败");
            throw new Exception("添加" + keyIndex + "号对称密钥失败");
        }
    }

}
