package com.spms.dbhsm.stockDataProcess.algorithm.encrypt;

import cn.hutool.core.codec.Base64;
import com.spms.dbhsm.stockDataProcess.algorithm.AlgorithmSPI;
import com.zayk.sdf.api.provider.ZaykJceGlobal;
import com.zayk.sdf.api.sdk.ZaykSDF;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/7/1 14:53
 */
@Slf4j
public class HardSM4 implements AlgorithmSPI {
    static {
        SDF = ZaykSDF.getInstance("/etc/zayk4j.ini");
    }

    @Getter
    private static final ZaykSDF SDF;

    //key
    private static final String KEY_INDEX = "key-index";

    private static final String IV = "iv";

    private static final String MODE = "mode";

    private static final int IV_LENGTH = 16;

    private static final Set<String> MODES = new HashSet<>(Arrays.asList("ECB", "CBC"));

    //value
    private int keyIndex;

    private byte[] sm4Iv;

    private int sm4Mode;

    @Override
    public String encrypt(String data, String key, Map<String, String> props) {
        log.info("encrypt data:{},key:{},props:{}", data, key, props);
        if (data.isEmpty()) {
            return "";
        }
        init(props);
        //加密
        byte[] bytes = SDF.SDF_Encrypt_Ex(Integer.parseInt(key), sm4Mode, new byte[0], sm4Iv, data.getBytes(StandardCharsets.UTF_8), true);
        return Base64.encode(bytes);
    }


    @Override
    public String decrypt(String data, String key, Map<String, String> props) {
        log.info("decrypt data:{},key:{},props:{}", data, key, props);
        if (data.isEmpty()) {
            return "";
        }
        init(props);
        //解密
        byte[] decode = Base64.decode(data);
        byte[] bytes = SDF.SDF_Decrypt_Ex(Integer.parseInt(key), sm4Mode, new byte[0], sm4Iv, decode, true);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public String getType() {
        return "TestAlg";
    }

    //private=====


    private void init(Map<String, String> props) {
        //参数
        sm4Mode = createSm4Mode(props);
//        keyIndex = createSm4Key(props);
        sm4Iv = createSm4Iv(props, sm4Mode);
    }

    private byte[] createSm4Iv(Map<String, String> props, int sm4Mode) {
//        if (sm4Mode == ZaykJceGlobal.SGD_SMS4_ECB) {
//            return new byte[0];
//        }
//        if (!props.containsKey(IV)) {
//            throw new RuntimeException("iv is not set");
//        }
//        String ivStr = props.get(IV);
//        byte[] result = fromHexString(ivStr);
//        if (result.length != IV_LENGTH) {
//            throw new RuntimeException("iv length must be " + IV_LENGTH);
//        }
//        return result;
        return new byte[0];
    }

    private int createSm4Key(Map<String, String> props) {
        if (!props.containsKey(KEY_INDEX)) {
            throw new RuntimeException("key-index is not set");
        }
        String keyIndexStr = props.get(KEY_INDEX);
        //todo 判断keyIndexStr是否合法
        return Integer.parseInt(keyIndexStr);
    }

    private int createSm4Mode(Map<String, String> props) {
//        if (!props.containsKey(MODE)) {
//            throw new RuntimeException("mode is not set");
//        }
//        String mode = props.get(MODE);
//        if (!MODES.contains(mode)) {
//            throw new RuntimeException("Mode must be either CBC or ECB");
//        }
//        switch (mode) {
//            case "CBC":
//                return ZaykJceGlobal.SGD_SMS4_CBC;
//            case "ECB":
//            default:
//                return ZaykJceGlobal.SGD_SMS4_ECB;
//        }
        return ZaykJceGlobal.SGD_SMS4_ECB;
    }

}
