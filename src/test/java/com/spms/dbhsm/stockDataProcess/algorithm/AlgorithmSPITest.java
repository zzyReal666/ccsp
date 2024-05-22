package com.spms.dbhsm.stockDataProcess.algorithm;

import com.spms.common.spi.typed.TypedSPIRegistry;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class AlgorithmSPITest{

    @Test
    public void testAlg() {
        Optional<AlgorithmSPI> testAlg = TypedSPIRegistry.findRegisteredService(AlgorithmSPI.class, "TestAlg");

        assert testAlg.isPresent();
        AlgorithmSPI algorithmSPI = testAlg.get();

       Map<String, String> prors = new HashMap<>();
       //put 数据
        prors.put("key1", "value1");
        prors.put("key2", "value2");
        prors.put("key3", "value3");
        prors.put("key4", "value4");

        String encrypt = algorithmSPI.encrypt("data1", "key1", prors);
        String decrypt = algorithmSPI.decrypt("data1", "key1", prors);

        System.out.println(encrypt);
        System.out.println(decrypt);
    }

}