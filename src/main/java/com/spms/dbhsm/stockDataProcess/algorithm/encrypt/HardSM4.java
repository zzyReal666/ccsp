package com.spms.dbhsm.stockDataProcess.algorithm.encrypt;

import cn.hutool.core.codec.Base64;
import com.spms.dbhsm.stockDataProcess.algorithm.AlgorithmSPI;
import com.zayk.sdf.api.provider.ZaykJceGlobal;
import com.zayk.sdf.api.sdk.ZaykSDF;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

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

    private static final String KEY_INDEX = "key-index";

    private byte[] sm4Iv;

    private int sm4Mode;

    @Override
    public String encrypt(String data, String key, Map<String, String> props) {
        if (null == data || data.isEmpty()) {
            return "";
        }
        init(props);
        //加密
        byte[] bytes = SDF.SDF_Encrypt_Ex(Integer.parseInt(key), sm4Mode, new byte[0], sm4Iv, data.getBytes(StandardCharsets.UTF_8), false);
        if (null == bytes || bytes.length == 0) {
            log.error("encrypt result isEmpty, data:{}, key:{}, props:{}", data, key, props);
            return "";
        }
        String result = Base64.encode(bytes);
        log.info("encrypt result:{}, data:{}, key:{}, props:{}",result, data, key, props);
        return result;
    }



    @Override
    public String decrypt(String data, String key, Map<String, String> props) {
        if (null == data || data.isEmpty()) {
            return "";
        }
        init(props);
        //解密
        byte[] decode = Base64.decode(data);
        byte[] bytes = SDF.SDF_Decrypt_Ex(Integer.parseInt(key), sm4Mode, new byte[0],sm4Iv, decode, false);
        if (null == bytes || bytes.length == 0) {
            log.error("decrypt result isEmpty, data:{}, key:{}, props:{}", data, key, props);
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public String getType() {
        return "TestAlg";
    }

    //private=====


    private void init(Map<String, String> props) {
        sm4Mode = createSm4Mode(props);
        sm4Iv = createSm4Iv(props, sm4Mode);
    }

    private byte[] createSm4Iv(Map<String, String> props, int sm4Mode) {
        return new byte[16];
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
        return ZaykJceGlobal.SGD_SMS4_OFB;
    }

}
