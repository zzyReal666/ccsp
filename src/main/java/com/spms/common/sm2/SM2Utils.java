package com.spms.common.sm2;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.KeyTools;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * @ClassName SM2Utils
 * @Description SM2算法工具类
 */
@Slf4j
public class SM2Utils {
    public static KeyPair createECKeyPair() {
        final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");

        // 获取一个椭圆曲线类型的密钥对生成器
        final KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            kpg.initialize(sm2Spec, new SecureRandom());

            return kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encrypt(String publicKeyHex, byte[] data) {
        return encrypt(getECPublicKeyByPublicKeyHex(publicKeyHex), data, 1);
    }

    public static String encrypt(BCECPublicKey publicKey, byte[] data, int modeType) {
        //加密模式
        SM2Engine.Mode mode = SM2Engine.Mode.C1C3C2;
        if (modeType != 1) {
            mode = SM2Engine.Mode.C1C2C3;
        }
        ECParameterSpec ecParameterSpec = publicKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());
        ECPublicKeyParameters ecPublicKeyParameters = new ECPublicKeyParameters(publicKey.getQ(), ecDomainParameters);

        SM2Engine sm2Engine = new SM2Engine(mode);

        sm2Engine.init(true, new ParametersWithRandom(ecPublicKeyParameters, new SecureRandom()));
        byte[] arrayOfBytes = null;
        try {
            arrayOfBytes = sm2Engine.processBlock(data, 0, data.length);
        } catch (Exception e) {
            System.out.println("SM2加密时出现异常:" + e.getMessage());
            e.printStackTrace();
        }
        return Hex.toHexString(arrayOfBytes);
    }

    public static byte[] decrypt(String privateKeyHex, String cipherData) {
        return decrypt(getBCECPrivateKeyByPrivateKeyHex(privateKeyHex), cipherData, 1);
    }

    public static byte[] decrypt(BCECPrivateKey privateKey, String cipherData, int modeType) {
        //解密模式
        SM2Engine.Mode mode = SM2Engine.Mode.C1C3C2;
        if (modeType != 1) {
            mode = SM2Engine.Mode.C1C2C3;
        }

        byte[] cipherDataByte = Hex.decode(cipherData);
        ECParameterSpec ecParameterSpec = privateKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());
        ECPrivateKeyParameters ecPrivateKeyParameters = new ECPrivateKeyParameters(privateKey.getD(),
                ecDomainParameters);

        SM2Engine sm2Engine = new SM2Engine(mode);
        sm2Engine.init(false, ecPrivateKeyParameters);
        byte[] result = null;
        try {
            byte[] arrayOfBytes = sm2Engine.processBlock(cipherDataByte, 0, cipherDataByte.length);
            result = arrayOfBytes;
        } catch (Exception e) {
            log.info("SM2解密时出现异常" + e.getMessage());
        }
        return result;
    }

    private static X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");

    private static ECParameterSpec ecDomainParameters = new ECParameterSpec(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN());

    public static BCECPublicKey getECPublicKeyByPublicKeyHex(String pubKeyHex) {

        if (pubKeyHex.length() > 128) {
            pubKeyHex = pubKeyHex.substring(pubKeyHex.length() - 128);
        }
        String stringX = pubKeyHex.substring(0, 64);
        String stringY = pubKeyHex.substring(stringX.length());
        BigInteger x = new BigInteger(stringX, 16);
        BigInteger y = new BigInteger(stringY, 16);

        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(x9ECParameters.getCurve().createPoint(x, y), ecDomainParameters);

        return new BCECPublicKey("EC", ecPublicKeySpec, BouncyCastleProvider.CONFIGURATION);
    }

    public static BCECPrivateKey getBCECPrivateKeyByPrivateKeyHex(String privateKeyHex) {
        BigInteger d = new BigInteger(privateKeyHex, 16);
        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(d, ecDomainParameters);
        return new BCECPrivateKey("EC", ecPrivateKeySpec, BouncyCastleProvider.CONFIGURATION);
    }

    public static void main(String[] args) throws Exception {
        String publicKeyHex = null;
        String privateKeyHex = null;
        KeyPair keyPair = createECKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        if (publicKey instanceof BCECPublicKey) {
            //获取65字节非压缩缩的十六进制公钥串(0x04)
            publicKeyHex = Hex.toHexString(((BCECPublicKey) publicKey).getQ().getEncoded(false));
            System.out.println("SM2公钥：" + publicKeyHex);
        }
        PrivateKey privateKey = keyPair.getPrivate();
        if (privateKey instanceof BCECPrivateKey) {
            //获取32字节十六进制私钥串
            privateKeyHex = ((BCECPrivateKey) privateKey).getD().toString(16);
            System.out.println("SM2私钥：" + privateKeyHex);
        }

        /**
         * 公钥加密
         */
        String data = "1234567890ABCDEF";

        //将十六进制公钥串转换为 BCECPublicKey 公钥对象
        String encryptData = encrypt(publicKeyHex, data.getBytes(StandardCharsets.UTF_8));
        System.out.println("加密结果：" + encryptData);

        /**
         * 私钥解密
         */
        //将十六进制私钥串转换为 BCECPrivateKey 私钥对象
        byte[] plain_data = decrypt(privateKeyHex, encryptData);
        System.out.println("解密结果：" + Hex.toHexString(plain_data));

        String encryptData1 = "MHkCIBQHCNDGKVBRDiKkQlJCISW6uzzrcUa5AfGhQldtYucFAiEAl79E+yGX01NRz6WRfe266rzfnG7mLudTcQlG/qHgB6IEIL7hLe99DWxlx5GgDznV1gX+t67g7X3B/EHklyyDzRKpBBAofYtkVRXm4HjZK50c2s8V";
        publicKeyHex = "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEgBIQBz1ayFZoRnw1mbdSEDm7fnA3Zt+x1nrolIib6cyS69+AcQCWWkYhGxpqlFf7SigE7jOVpM+RcGOsWI2pGw==";
        privateKeyHex = "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgCulB6/CHlSV7STb+bIUheSfdw5hgHyvhR7R4fIDliiGgCgYIKoEcz1UBgi2hRANCAASAEhAHPVrIVmhGfDWZt1IQObt+cDdm37HWeuiUiJvpzJLr34BxAJZaRiEbGmqUV/tKKATuM5Wkz5FwY6xYjakb";

        byte[] byte_data = decrypt_jit(privateKeyHex, encryptData1);
        System.out.println("解密结果：" + Hex.toHexString(byte_data));
    }


    public static byte[] decrypt_jit(String privateKeyHex, String encryptData1) throws Exception {
        String encryptData;
        ASN1Sequence seq  = ASN1Sequence.getInstance(Base64.decode(encryptData1));
        if (seq.size()  != 4) {
            log.info("decrypt_jit--ERROR");
            throw new ZAYKException("数字信封解析失败！");
        }
        ASN1Integer x1 = (ASN1Integer) seq.getObjectAt(0);
        ASN1Integer y1 = (ASN1Integer) seq.getObjectAt(1);
        ASN1OctetString M3 = (ASN1OctetString) seq.getObjectAt(2);
        ASN1OctetString C2 = (ASN1OctetString) seq.getObjectAt(3);

        byte[] byte_X1 = BigIntegers.asUnsignedByteArray(x1.getValue());
        byte[] byte_Y1 = BigIntegers.asUnsignedByteArray(y1.getValue());
        byte[] byte_M3 = M3.getOctets();
        byte[] byte_C2 = C2.getOctets();
        byte[] byte_ALL = new byte[96 + byte_C2.length + 1];

        byte_ALL[0] = 4;
        System.arraycopy(byte_X1, 0, byte_ALL,33 - byte_X1.length, byte_X1.length);
        System.arraycopy(byte_Y1, 0, byte_ALL,65 - byte_Y1.length, byte_Y1.length);
        System.arraycopy(byte_M3, 0, byte_ALL,97 - byte_M3.length, byte_M3.length);
        System.arraycopy(byte_C2, 0, byte_ALL,97, byte_C2.length);

        encryptData = Hex.toHexString(byte_ALL);
        BCECPrivateKey privateKey_sig = KeyTools.convertPKCS8ToECPrivateKey(Base64.decode(privateKeyHex));
        byte[] byte_data = decrypt(privateKey_sig, encryptData, 1);
        return byte_data;
    }
}
