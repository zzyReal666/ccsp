package com.spms.common.kmip;

import com.sc.kmip.attributes.*;
import com.sc.kmip.client.KMSClient;
import com.sc.kmip.client.KMSClientInterface;
import com.sc.kmip.container.KMIPContainer;
import com.sc.kmip.kmipenum.*;
import com.sc.kmip.objects.Authentication;
import com.sc.kmip.objects.CredentialValue;
import com.sc.kmip.objects.base.Attribute;
import com.sc.kmip.objects.base.Credential;
import com.sc.kmip.process.decoder.KMIPDecoderException;
import com.sc.kmip.types.KMIPDateTime;
import com.sc.kmip.types.KMIPTextString;
import com.sc.kmip.types.KMIPType;
import com.spms.common.Int4jUtil;
import com.spms.common.constant.DbConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * @author diq
 * @ClassName ZaKmipUtil
 * @date 2022-11-15
 */
@Slf4j
@Configuration
public class ZaKmipUtil {

    /**
     * 生成对称密钥
     *
     * @param alg
     * @param keyLength
     * @param keyName
     * @return
     */
    public static KMIPContainer createSymmetricKey(KMSClientInterface client,int alg, int keyLength, String keyName) {
        Authentication authentication = null;

        ArrayList<Attribute> templateAttributes = new ArrayList<Attribute>();
        //开始使用时间
        Calendar calendar = Calendar.getInstance();
        templateAttributes.add(new ProcessStartDate(new KMIPDateTime(calendar.getTime())));
        calendar.add(Calendar.DATE, 1);
        //结束使用时间
        templateAttributes.add(new ProtectStopDate(new KMIPDateTime(calendar.getTime())));

        templateAttributes.add(new ActivationDate(new KMIPDateTime(new Date())));

        templateAttributes.add(new CryptographicAlgorithm(alg));
        templateAttributes.add(new CryptographicLength(keyLength));
        templateAttributes.add(new CryptographicUsageMask(127));

        if (!"".equals(keyName) && keyName != null) {
            KMIPType name = new KMIPTextString(keyName);
            templateAttributes.add(new Name(name, EnumNameType.UninterpretedTextString));
        }
        CryptographicParameters cryptographicParameters = new CryptographicParameters();
        cryptographicParameters.setBlockCipherMode(new EnumBlockCipherMode(0x02));  //ECB
        cryptographicParameters.setPaddingMethod(new EnumPaddingMethod(0x03));  //PKCS5
        KMIPContainer response = null;
        try {
            response = client.createSymmetricKey(authentication, templateAttributes, cryptographicParameters);
        } catch (ConnectException e) {
            e.printStackTrace();
        } catch (KMIPDecoderException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 获取数字信封格式密钥
     * <p>
     * 0009格式
     *
     * @param kid        密钥唯一标识符
     * @param pubKeyId   包裹公钥的唯一标识符
     * @param pubKeyType 包裹公钥的类型
     * @param username 认证用户名
     * @param password 认证密码
     * @throws ConnectException
     * @throws KMIPDecoderException
     */
    public static KMIPContainer wrapKey(KMSClientInterface client,String kid, String pubKeyId, String pubKeyType,String username, String password) {
        Authentication authentication = null;
        KMIPContainer response = null;
        if ((!username.equals("")) && (!username.equals(" ")) && username != null && password != null) {
            Credential credential = new Credential(new EnumCredentialType("UsernameAndPassword"),
                    new CredentialValue(username, password));
            authentication = new Authentication(credential);
        }
        try {
            switch (pubKeyType.toUpperCase()) {
                case "SM2":
                    response = client.get(authentication, kid, EnumWrappingMethod.Encrypt, pubKeyId, EnumBlockCipherMode.ECB, EnumPaddingMethod.None, EnumHashingAlgorithm.SHA256, EnumKeyRoleType.KEK);
                    break;
                case "RSA":
                    response = client.get(authentication, kid, EnumWrappingMethod.Encrypt, pubKeyId, EnumBlockCipherMode.ECB, EnumPaddingMethod.PKCS1, EnumHashingAlgorithm.SHA256, EnumKeyRoleType.KEK);
                    break;
                default:
                    break;
            }
        } catch (KMIPDecoderException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            e.printStackTrace();
        }
        return response;
    }

    public static KMSClientInterface  reconnKMIP(String fileName) throws IOException {
        KMSClientInterface client = new KMSClient(DbConstants.KMIP_INI_PATH +"/"+fileName+".ini");
        if (client == null) {
            String ip = Int4jUtil.getValue(DbConstants.KMIP_INI_PATH +"/"+fileName+".ini",DbConstants.KMC1,"ip");
            String port = Int4jUtil.getValue(DbConstants.KMIP_INI_PATH +"/"+fileName+".ini", DbConstants.KMC1,"KMIP_Port");
            log.error("Kmip connect error: url : " + ip + port);
            return null;
        }else {
            log.info("Kmip connect success");
            return client;
        }
    }

}
