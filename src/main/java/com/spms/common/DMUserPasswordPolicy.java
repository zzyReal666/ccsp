package com.spms.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 18853
 * 达梦数据库口令策略
 */
public class DMUserPasswordPolicy {
    public static String getPolicyRules(int value) {
        List<String> rules = new ArrayList<>();
        if (value == 0) {
            rules.add("无策略");
        } else {
            int sum = 0;
            if ((value & 1) == 1) {
                rules.add("禁止与用户名相同");
                sum += 1;
            }
            if ((value & 2) == 2) {
                rules.add("口令长度不小于 9");
                sum += 2;
            }
            if ((value & 4) == 4) {
                rules.add("至少包含一个大写字母（A-Z）");
                sum += 4;
            }
            if ((value & 8) == 8) {
                rules.add("至少包含一个数字（0－9）");
                sum += 8;
            }
            if ((value & 16) == 16) {
                rules.add("至少包含一个标点符号（英文输入法状态下，除“和空格外的所有符号）");
                sum += 16;
            }
            //if (value != 1 && value != 2 && value != 4 && value != 8 && value != 16) {
            //    rules.add("同时启用第" + sum + "规则");
            //}
        }
        return String.join(", ", rules);
    }
    public static void main(String[] args) {
        int value = 9;
        String rules = DMUserPasswordPolicy.getPolicyRules(value);
        System.out.println("Password Policy Rules for value " + value + ":");
            System.out.println(rules);
    }

}
