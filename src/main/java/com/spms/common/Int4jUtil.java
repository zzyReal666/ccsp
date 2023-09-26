package com.spms.common;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class Int4jUtil {
    public static String getValue(String iniFile, String sec, String key) throws IOException {
        Ini ini = new Ini();
        ini.load(new File(iniFile));

        Section section = ini.get(sec);
        return section.get(key);
    }

    /**
     *
     * @param iniFile 文件路径
     * @param sec key value所属的【分类条目】值
     * @param key
     * @param value
     * @throws IOException
     */
    public static void setValue(String iniFile, String sec, String key, String value) throws IOException {
        Ini ini = new Ini();
        ini.load(new File(iniFile));

        Section section = ini.get(sec);
        section.put(key, value);

        ini.store(new File(iniFile));
    }

}
