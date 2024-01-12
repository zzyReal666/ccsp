package com.spms.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 18853
 * 达梦数据库口令策略
 */
public class DMUserPasswordPolicy {
    public static String validatePolicyRules(int value, String inputPassword, String userName) {
        List<String> rules = new ArrayList<>();
        if (value == 0) {
            rules.add("无策略");
        } else {
            int sum = 0;
            if ((value & 1) == 1) {
                sum += 1;
                if  (inputPassword==userName) {
                    rules.add("禁止与用户名相同");
                }
            }
            if ((value & 2) == 2) {
                sum += 2;
                if (inputPassword.length() < 9 || inputPassword.length() > 50) {
                    rules.add("密码长度范围[9-50]");
                }
            }
            if ((value & 4) == 4) {
                sum += 4;
                if (!inputPassword.matches("^(?=.*[A-Z]).*$")) {
                    rules.add("至少包含一个大写字母（A-Z）");
                }

            }
            if ((value & 8) == 8) {
                sum += 8;
                if (!inputPassword.matches("^(?=.*[A-Z]).*$")) {
                    rules.add("至少包含一个数字（0－9）");
                }

            }
            if ((value & 16) == 16) {
                rules.add("至少包含一个标点符号（英文输入法状态下，除“和空格外的所有符号）");
                sum += 16;
                if(!inputPassword.matches("(?=.*?[!\\\"#$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~]).*")){
                    rules.add("至少包含一个标点符号（英文输入法状态下，除“和空格外的所有符号）");
                }
            }
        }
        return rules.size()>0? String.join(", ", rules):"true";
    }
    public static void main(String[] args) {
        int value = 2;
        String rules = DMUserPasswordPolicy.validatePolicyRules(value, "123467890123467890123467890123467890123467890111113","Usernaem");
        System.out.println("Password Policy Rules for value " + value + ":");
            System.out.println(rules);
    }

}
