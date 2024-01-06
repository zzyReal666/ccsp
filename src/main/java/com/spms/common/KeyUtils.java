package com.spms.common;

import com.ccsp.cert.provider.sinocipher.CurveUtils;
import com.ccsp.cert.provider.sinocipher.RSArefPublicKey;
import com.ccsp.common.core.utils.StringUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Base64;
import org.cesecore.util.CertTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Enumeration;

public class KeyUtils {
    public final static int CA_TYPE_RSA = 0;
    public final static int CA_TYPE_SM2 = 1;
    public final static int CA_TYPE_ECC = 2;

    public final static String SIGN_ALG_SM3WITHSM2 = "SM3WITHSM2";
    public final static String SIGN_ALG_SHA256WITHRSA = "SHA256WITHRSA";
    public final static String SIGN_ALG_SHA256WITHECDSA = "SHA256WITHECDSA";

    public final static String KEY_TYPE_RSA = "RSA";
    public final static String KEY_TYPE_SM2 = "SM2";
    public final static String KEY_TYPE_ECC = "ECC";

    public final static int SM2_KEY_LEN = 32;

    public final static int RSA_KEY_TYPE_1024 = 1;
    public final static int RSA_KEY_TYPE_2048 = 2;
    public final static int RSA_KEY_TYPE_3072 = 3;
    public final static int RSA_KEY_TYPE_4096 = 4;

    public final static int KEY_BIT_LEN_BASE = 1024;
    public final static int KEY_BIT_LEN_1024 = RSA_KEY_TYPE_1024 * KEY_BIT_LEN_BASE;
    public final static int KEY_BIT_LEN_2048 = RSA_KEY_TYPE_2048 * KEY_BIT_LEN_BASE;
    public final static int KEY_BIT_LEN_3072 = RSA_KEY_TYPE_3072 * KEY_BIT_LEN_BASE;
    public final static int KEY_BIT_LEN_4096 = RSA_KEY_TYPE_4096 * KEY_BIT_LEN_BASE;

    public final static int ECC_CURVE_B_163 = 0;
    public final static int ECC_CURVE_B_233 = 1;
    public final static int ECC_CURVE_B_283 = 2;
    public final static int ECC_CURVE_B_409 = 3;
    public final static int ECC_CURVE_B_571 = 4;
    public final static int ECC_CURVE_K_163 = 5;
    public final static int ECC_CURVE_K_233 = 6;
    public final static int ECC_CURVE_K_283 = 7;
    public final static int ECC_CURVE_K_409 = 8;
    public final static int ECC_CURVE_K_571 = 9;
    public final static int ECC_CURVE_P_192 = 10;
    public final static int ECC_CURVE_P_224 = 11;
    public final static int ECC_CURVE_P_256 = 12;
    public final static int ECC_CURVE_P_384 = 13;
    public final static int ECC_CURVE_P_521 = 14;


    public static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {
        Security.addProvider(new BouncyCastleProvider());

        BouncyCastleProvider bouncyCastleProvider = ((BouncyCastleProvider) Security.getProvider(BC));

        bouncyCastleProvider.addKeyInfoConverter(PKCSObjectIdentifiers.rsaEncryption, new org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi());
        bouncyCastleProvider.addKeyInfoConverter(X9ObjectIdentifiers.id_ecPublicKey, new org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi.EC());

    }

    private static final Logger log = LoggerFactory.getLogger(KeyUtils.class);

    public static X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");
    public static ECDomainParameters ecDomainParameters = new ECDomainParameters(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN());
    //private static ECParameterSpec ecParamterSpec = new ECParameterSpec(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN());
//    public static java.security.spec.ECParameterSpec ecParamterSpec = EC5Util.convertToSpec(x9ECParameters);

    public static ECNamedCurveSpec ecNamedCurveSpec = new ECNamedCurveSpec("sm2p256v1", ecDomainParameters.getCurve(), ecDomainParameters.getG(), ecDomainParameters.getN(), ecDomainParameters.getH(), ecDomainParameters.getSeed());

    public static KeyPair generateRSAKeyPair(int keySize) throws Exception {
        SecureRandom random = new SecureRandom();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BC);

        keyPairGenerator.initialize(keySize, random);

        return keyPairGenerator.generateKeyPair();
    }

    public static KeyPair generateECCKeyPair() throws Exception {
        SecureRandom random = new SecureRandom();
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BC);

        keyPairGenerator.initialize(ecSpec, random);

        return keyPairGenerator.generateKeyPair();
    }

    public static KeyPair generateSM2KeyPair() throws Exception {
        SecureRandom random = new SecureRandom();
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("sm2p256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BC);

        keyPairGenerator.initialize(ecSpec, random);

        return keyPairGenerator.generateKeyPair();
    }

    public static PublicKey getPublicKey(SubjectPublicKeyInfo subjectPublicKeyInfo) throws Exception {
        return BouncyCastleProvider.getPublicKey(subjectPublicKeyInfo);
    }

    public static ECPublicKey convert2SM2PublicKey(byte[] publickey) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        if (publickey.length != SM2_KEY_LEN * 2 && publickey.length != SM2_KEY_LEN * 2 + 1) {
            log.error("publickey Size Error:" + publickey.length);
            throw new IllegalArgumentException("publickey Size Error:" + publickey.length);
        }

        return convert2SM2PublicKey(Arrays.copyOfRange(publickey, publickey.length - SM2_KEY_LEN * 2, publickey.length - SM2_KEY_LEN),
                Arrays.copyOfRange(publickey, publickey.length - SM2_KEY_LEN, publickey.length));
    }

    public static ECPublicKey convert2EccPublicKey(byte[] publickey, int curve) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        int length = CurveUtils.getCurveLengthById(curve);
        return convert2EccPublicKey(Arrays.copyOfRange(publickey, publickey.length - length * 2, publickey.length - length),
                Arrays.copyOfRange(publickey, publickey.length - length, publickey.length), curve);
    }


    public static ECPublicKey convert2SM2PublicKey(byte[] publickeyX, byte[] publickeyY) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyFactory factory = KeyFactory.getInstance("EC", BC);

        if (publickeyX.length != SM2_KEY_LEN || publickeyY.length != SM2_KEY_LEN) {
            log.error("publickey Size Error:" + publickeyX.length + ":" + publickeyY.length);
            new IllegalArgumentException("publickey Size Error:" + publickeyX.length + ":" + publickeyY.length);
        }

        BigInteger X = new BigInteger(1, publickeyX);
        BigInteger Y = new BigInteger(1, publickeyY);

        ECPoint point = new ECPoint(X, Y);

        ECPublicKeySpec keySpec = new ECPublicKeySpec(point, ecNamedCurveSpec); //ecParamterSpec;
        return (ECPublicKey) factory.generatePublic(keySpec);
    }

    public static ECPublicKey convert2EccPublicKey(byte[] publickeyX, byte[] publickeyY, int curve) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyFactory factory = KeyFactory.getInstance("EC", BC);

        BigInteger X = new BigInteger(1, publickeyX);

        BigInteger Y = new BigInteger(1, publickeyY);

        ECPoint point = new ECPoint(X, Y);

        String name = CurveUtils.getCurveNamehById(curve);
        X9ECParameters ecParameters = CustomNamedCurves.getByName(name);

        ECDomainParameters domainParameters = new ECDomainParameters(ecParameters.getCurve(), ecParameters.getG(), ecParameters.getN());

        ECNamedCurveSpec namedCurveSpec = new ECNamedCurveSpec(name, domainParameters.getCurve(), domainParameters.getG(), domainParameters.getN(), domainParameters.getH(), domainParameters.getSeed());

        ECPublicKeySpec keySpec = new ECPublicKeySpec(point, namedCurveSpec); //namedCurveSpec;
        return (ECPublicKey) factory.generatePublic(keySpec);
    }

    public static ECPrivateKey convert2SM2PrivateKey(byte[] privatekey) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyFactory factory = KeyFactory.getInstance("EC", BC);

        if (privatekey.length != SM2_KEY_LEN) {
            throw new IllegalArgumentException("publickey Size Error:" + privatekey.length);
        }

        ECPrivateKeySpec keySpec = new ECPrivateKeySpec(new BigInteger(1, privatekey), ecNamedCurveSpec); //ecParamterSpec;
        return (ECPrivateKey) factory.generatePrivate(keySpec);
    }

    /**
     * @param x 32位
     * @param y 32位
     * @param d 32位
     * @return
     * @throws IOException
     */
    public static byte[] convert2SM2PrivateKey(byte[] x, byte[] y, byte[] d) throws IOException {
        ASN1EncodableVector pri = new ASN1EncodableVector();
        pri.add(new ASN1Integer(1));
        if (d.length == 32) {
            pri.add(new DEROctetString(d));
        } else {
            byte[] dKey = new byte[32];
            System.arraycopy(d, 32, dKey, 0, SM2_KEY_LEN);
            pri.add(new DEROctetString(dKey));
        }
        pri.add(new DERTaggedObject(true, 0, GMObjectIdentifiers.sm2p256v1));
        byte[] publicKey = new byte[65];
        publicKey[0] = 0x04;
        System.arraycopy(x, 0, publicKey, 1, SM2_KEY_LEN);
        System.arraycopy(y, 0, publicKey, 1 + SM2_KEY_LEN, SM2_KEY_LEN);
        pri.add(new DERTaggedObject(true, 1, new DERBitString(publicKey)));

        return new DERSequence(pri).getEncoded();
    }

    public static ECPrivateKey convert2SM2PrivateKey2(byte[] privatekey)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyFactory factory = KeyFactory.getInstance("EC", BC);
        PrivateKey pKey = null;
        try {
            ECPrivateKeySpec keySpec = null;
            // 判断是否是32位的私钥
            if (privatekey.length <= SM2_KEY_LEN) {
                keySpec = new ECPrivateKeySpec(new BigInteger(1, privatekey), ecNamedCurveSpec); // ecParamterSpec;
            } else {
                BigInteger key = null;

                org.bouncycastle.asn1.sec.ECPrivateKey ec = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(privatekey);
                key = ec.getKey();
                keySpec = new ECPrivateKeySpec(key, ecNamedCurveSpec); // ecParamterSpec;
            }

            pKey = factory.generatePrivate(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 兼容PKCS8标准的私钥
        if (pKey == null) {
            try {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privatekey);
                pKey = factory.generatePrivate(keySpec);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pKey == null) {
            // 兼容JDK ECPrivateKey 私钥
            try {
                ASN1Sequence instance = ASN1Sequence.getInstance(privatekey);
                ASN1Encodable encodable =  instance.getObjectAt(2);
                ASN1OctetString octetString = (ASN1OctetString)encodable;
                ASN1Sequence instance1 = ASN1Sequence.getInstance(octetString.getOctets());
                ASN1Integer objectAt = (ASN1Integer) instance1.getObjectAt(1);
                BigInteger value = objectAt.getValue();
                byte[] bytes = BigIntegers.asUnsignedByteArray(value);
                ECPrivateKeySpec keySpec = null;
                keySpec = new ECPrivateKeySpec(new BigInteger(1, bytes), ecNamedCurveSpec);
                pKey = factory.generatePrivate(keySpec);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pKey == null) {

            throw new IllegalArgumentException("privatekey type Error:" + privatekey.length);
        }
        return (ECPrivateKey) pKey;
    }

    public static RSAPublicKey convert2RSAPublicKey(byte[] publickey) throws Exception {
        int keytype = publickey[0];
        if (keytype != RSA_KEY_TYPE_1024 && keytype != RSA_KEY_TYPE_2048 && keytype != RSA_KEY_TYPE_4096) {
            throw new Exception("KeyType Error:" + keytype);
        }

        int keylen = keytype * KEY_BIT_LEN_BASE / 8;

        byte[] modulus = Arrays.copyOfRange(publickey, 1, keylen + 1);
        byte[] publicExponent = Arrays.copyOfRange(publickey, keylen + 1, publickey.length);

        return convert2RSAPublicKey(modulus, publicExponent);
    }

    public static RSArefPublicKey convert2RSArefPublicKey(byte[] publickey) throws Exception {
        int keytype = publickey[0];
        if (keytype != RSA_KEY_TYPE_1024 && keytype != RSA_KEY_TYPE_2048 && keytype != RSA_KEY_TYPE_4096) {
            throw new Exception("KeyType Error:" + keytype);
        }
        int keylen = keytype * KEY_BIT_LEN_BASE / 8;
        byte[] range = Arrays.copyOfRange(publickey, 1, publickey.length);
        ASN1InputStream aIn = new ASN1InputStream(range);

        ASN1Primitive derObj = aIn.readObject();
        ASN1Sequence seq = (ASN1Sequence) derObj;
        Enumeration e = seq.getObjects();
        BigInteger value = ((ASN1Integer) e.nextElement()).getValue();
        byte[] modulus = value.toByteArray();

        byte[] publicExponent = ((ASN1Integer) e.nextElement()).getValue().toByteArray();

        RSArefPublicKey rsArefPublicKey = new RSArefPublicKey();
        rsArefPublicKey.bits = keylen * 8;
        System.arraycopy(modulus, modulus.length - keylen, rsArefPublicKey.m, 256 - keylen, keylen);
        System.arraycopy(publicExponent, 0, rsArefPublicKey.e, 256 - publicExponent.length, publicExponent.length);
        return rsArefPublicKey;

    }


    public static PublicKey convert2P8RSAPublicKey(byte[] publickey) throws Exception {
        try {

            byte[] bytes = GetPKCS8Data(publickey);
            // 创建 已编码的公钥规格
            X509EncodedKeySpec encPubKeySpec = new X509EncodedKeySpec(bytes);
            // 获取指定算法的密钥工厂, 根据 已编码的公钥规格, 生成公钥对象
            PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(encPubKeySpec);
            return pubKey;
        } catch (Exception e) {
            log.error("生成RSA公钥钥失败原因", e.getMessage());
            e.printStackTrace();
            throw new Exception(String.format("生成RSA公钥钥失败原因如【%s】", new Object[]{e.getMessage()}));
        }

    }


    public static RSAPublicKey convert2RSAPublicKey(byte[] modulus, byte[] publicExponent)
            throws Exception {
        if (modulus.length != KEY_BIT_LEN_1024 / 8 && modulus.length != KEY_BIT_LEN_2048 / 8 &&
                modulus.length != KEY_BIT_LEN_4096 / 8) {
            throw new Exception("modulus Length Error:" + modulus.length);
        }

//		if(publicExponent.length!=4)
//		{
//			throw new Exception("publicExponent Length Error:" + publicExponent.length);
//		}

        BigInteger bim = new BigInteger(1, modulus);
        BigInteger bip = new BigInteger(1, publicExponent);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(bim, bip);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }


    public static RSAPrivateKey convert2RSAPrivateKey(byte[] privatekey) throws Exception {

        int keytype = privatekey[0];
        if (keytype != RSA_KEY_TYPE_1024 && keytype != RSA_KEY_TYPE_2048 && keytype != RSA_KEY_TYPE_4096) {
            throw new Exception("KeyType Error:" + keytype);
        }

        int keylen = keytype * KEY_BIT_LEN_BASE / 8;

        byte[] modulus = Arrays.copyOfRange(privatekey, 1, keylen + 2);
        byte[] privateExponent = Arrays.copyOfRange(privatekey, keylen + 2, privatekey.length);

        return convert2RSAPrivateKey(modulus, privateExponent);
    }

    public static RSAPrivateKey convert2RSAPrivateKey(byte[] modulus, byte[] privateExponent) throws Exception {
//        if (modulus.length != KEY_BIT_LEN_1024 / 8 && modulus.length != KEY_BIT_LEN_2048 / 8 &&
//                modulus.length != KEY_BIT_LEN_4096 / 8) {
//            throw new Exception("modulus Length Error:" + modulus.length);
//        }
//
//        if (privateExponent.length != KEY_BIT_LEN_1024 / 8 && privateExponent.length != KEY_BIT_LEN_2048 / 8 &&
//                privateExponent.length != KEY_BIT_LEN_4096 / 8) {
//            throw new Exception("privateExponent Length Error:" + privateExponent.length);
//        }

        BigInteger bim = new BigInteger(1, modulus);
        BigInteger bip = new BigInteger(1, privateExponent);

        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(bim, bip);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    public static byte[] convert2PublicKeyByte(PublicKey pubKey) throws Exception {
        byte[] bpk = null;

        if (pubKey instanceof RSAPublicKey) {
            byte[] btmp1 = ((RSAPublicKey) pubKey).getModulus().toByteArray();
            byte[] btmp2 = ((RSAPublicKey) pubKey).getPublicExponent().toByteArray();
            int keytype = btmp1.length * 8 / KEY_BIT_LEN_BASE;

            if (keytype != RSA_KEY_TYPE_1024 && keytype != RSA_KEY_TYPE_2048 && keytype != RSA_KEY_TYPE_4096) {
                throw new Exception("Pubkey Modulus Len Error:" + btmp1.length);
            }

            bpk = new byte[btmp1.length + btmp2.length + 1];
            bpk[0] = (byte) keytype;
            System.arraycopy(btmp1, 0, bpk, 1, btmp1.length);
            System.arraycopy(btmp2, 0, bpk, 1 + btmp1.length, btmp2.length);
        } else if (pubKey instanceof ECPublicKey) {
            bpk = new byte[SM2_KEY_LEN * 2 + 1];
            bpk[0] = 0x04;
            byte[] btmp = ((ECPublicKey) pubKey).getW().getAffineX().toByteArray();
            System.arraycopy(btmp, btmp.length - SM2_KEY_LEN, bpk, 1, SM2_KEY_LEN);
            btmp = ((ECPublicKey) pubKey).getW().getAffineY().toByteArray();
            System.arraycopy(btmp, btmp.length - SM2_KEY_LEN, bpk, 1 + SM2_KEY_LEN, SM2_KEY_LEN);
        } else {
            throw new Exception("PublicKey Type Error:" + pubKey.getClass());
        }

        return bpk;
    }

    public static byte[] convert2PrivateKeyByte(PrivateKey priKey)
            throws Exception {
        byte[] bpk = null;

        if (priKey instanceof RSAPrivateKey) {
            byte[] btmp1 = ((RSAPrivateKey) priKey).getModulus().toByteArray();
            byte[] btmp2 = ((RSAPrivateKey) priKey).getPrivateExponent().toByteArray();
            int keytype = btmp1.length * 8 / KEY_BIT_LEN_BASE;

            if (keytype != RSA_KEY_TYPE_1024 && keytype != RSA_KEY_TYPE_2048 && keytype != RSA_KEY_TYPE_4096) {
                throw new Exception("Private Modulus Len Error:" + btmp1.length);
            }

            bpk = new byte[btmp1.length + btmp2.length + 1];
            bpk[0] = (byte) keytype;
            System.arraycopy(btmp1, 0, bpk, 1, btmp1.length);
            System.arraycopy(btmp2, 0, bpk, 1 + btmp1.length, btmp2.length);
        } else if (priKey instanceof ECPrivateKey) {
            bpk = new byte[SM2_KEY_LEN];

            byte[] btmp = ((ECPrivateKey) priKey).getS().toByteArray();
            System.arraycopy(btmp, 0, bpk, 0, SM2_KEY_LEN);
        } else {
            throw new Exception("PrivateKey Type Error:" + priKey.getClass());
        }

        return bpk;
    }

    public static PublicKey getRSAPublicKey(byte[] bytePuk) throws Exception {
        try {
            // 创建 已编码的公钥规格
            X509EncodedKeySpec encPubKeySpec = new X509EncodedKeySpec(bytePuk);
            // 获取指定算法的密钥工厂, 根据 已编码的公钥规格, 生成公钥对象
            PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(encPubKeySpec);
            return pubKey;
        } catch (Exception e) {
            log.error("生成RSA公钥钥失败原因", e.getMessage());
            e.printStackTrace();
            throw new Exception(String.format("生成RSA公钥钥失败原因如【%s】", new Object[]{e.getMessage()}));
        }
    }

    public static PublicKey getSM2PublicKey(byte[] bytePuk) throws Exception {
        try {
            // 创建 已编码的公钥规格
            X509EncodedKeySpec encPubKeySpec = new X509EncodedKeySpec(bytePuk);
            // 获取指定算法的密钥工厂, 根据 已编码的公钥规格, 生成公钥对象
            PublicKey pubKey = KeyFactory.getInstance("EC", BC).generatePublic(encPubKeySpec);
            return pubKey;
        } catch (Exception e) {
            log.error("生成RSA公钥钥失败原因", e.getMessage());
            e.printStackTrace();
            throw new Exception(String.format("生成SM2公钥钥失败原因如【%s】", new Object[]{e.getMessage()}));
        }
    }

    static byte[] GetPKCS8Data(byte[] p1Data) {
        try {
            ASN1EncodableVector id = new ASN1EncodableVector();
            id.add(new ASN1ObjectIdentifier("1.2.840.113549.1.1.1"));
            id.add(DERNull.INSTANCE);

            ASN1EncodableVector out = new ASN1EncodableVector();
            out.add(new DERSequence(id));
            out.add(new DERBitString(p1Data));

            byte[] outDer = new DERSequence(out).getEncoded();
            return outDer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析uKey公钥
     *
     * @author xuyz
     * @date 2022/10/26 10:31
     **/
    public static String userPublicKeyDispose(String userPublicKey) {
        byte[] decode = Base64.decode(userPublicKey);
        byte[] decodeRes = new byte[64];
        System.arraycopy(decode, 4, decodeRes, 0, 64);
        return Base64.toBase64String(decodeRes);
    }


    public static String replacePrivateKey(String privateKey) {
        if (StringUtils.isEmpty(privateKey)) {
            return privateKey;
        }
        return privateKey.replaceAll("-----BEGIN SM2 ENVELOPEDKEY-----", "")
                .replaceAll("-----END SM2 ENVELOPEDK" +
                        "EY-----", "")
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("-----BEGIN ENCRYPTED PRIVATE KEY-----", "")
                .replaceAll("-----END ENCRYPTED PRIVATE KEY-----", "")
                .replaceAll("-----BEGIN EC PRIVATE KEY-----", "")
                .replaceAll("-----END EC PRIVATE KEY-----", "")
                .replaceAll("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

    }

    public static boolean SignVerify(String algorithm ,byte[] cert, PrivateKey ecPrivateKey) {
        try {
            String orgData = "1234567890";
            Signature sig = Signature.getInstance(algorithm, new BouncyCastleProvider());
            sig.initSign(ecPrivateKey);
            sig.update(orgData.getBytes());
            byte[] rs = sig.sign();
            X509Certificate x509EncCert = CertTools.getCertfromByteArray(cert, X509Certificate.class);
            Signature verify = Signature.getInstance(algorithm, new BouncyCastleProvider());
            verify.initVerify(x509EncCert);
            verify.update(orgData.getBytes());
            boolean res = verify.verify(rs);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

