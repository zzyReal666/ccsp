package com.spms.dbhsm.stockDataProcess.algorithm.encrypt;

import cn.hutool.core.codec.Base64;
import com.zayk.sdf.api.provider.ZaykJceGlobal;
import com.zayk.sdf.api.sdk.ZaykSDF;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class HardSM4Test {


    @Test
    public void testEncrypt() {
        HardSM4 hardSM4 = new HardSM4();
        String zzy123 = hardSM4.encrypt("zzy123", "1", null);
        String decrypt = hardSM4.decrypt(zzy123, "1", null);

        System.out.println("zzy123: " + zzy123);
        System.out.println("decrypt: " + decrypt);
    }

    @Test
    public void testEncrypt2() {
        byte[] bytes = new byte[] {-119,-87,77,92,64};
        System.out.println(new String(bytes, StandardCharsets.UTF_8));

        ZaykSDF  SDF = ZaykSDF.getInstance("/etc/zayk4j.ini");
        byte[] bytes1 = SDF.SDF_Decrypt_Ex(0, ZaykJceGlobal.SGD_SMS4_OFB, Base64.decode("q2x+FJmMX1tTOoVXpk0U+g=="), new byte[16], bytes, false);
        String s = new String(bytes1, StandardCharsets.UTF_8);
        System.out.println(s);
    }

}