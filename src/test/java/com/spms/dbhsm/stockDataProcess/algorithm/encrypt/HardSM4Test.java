package com.spms.dbhsm.stockDataProcess.algorithm.encrypt;

import org.junit.Test;

import static org.junit.Assert.*;

public class HardSM4Test {


    @Test
    public void testEncrypt() {
        HardSM4 hardSM4 = new HardSM4();
        String zzy123 = hardSM4.encrypt("zzy123", "1", null);
        String decrypt = hardSM4.decrypt(zzy123, "1", null);

        System.out.println("zzy123: " + zzy123);
        System.out.println("decrypt: " + decrypt);


    }

}