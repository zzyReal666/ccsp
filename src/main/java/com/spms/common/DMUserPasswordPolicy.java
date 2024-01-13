package com.spms.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 18853
 * 达梦数据库口令策略
 */
public class DMUserPasswordPolicy {
    public static String validatePolicyRules(int value, String inputPassword, String userName,int pwdMinLenToDM) {
        String regex;
        Pattern pattern;
        Matcher matcher;
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
                if (inputPassword.length() < pwdMinLenToDM || inputPassword.length() > 50) {
                    rules.add("密码长度范围["+pwdMinLenToDM+"-50]");
                }
            }
            if ((value & 4) == 4) {
                sum += 4;
                regex = "^(?=.*[A-Z]).*$";
                pattern = Pattern.compile(regex);
                matcher = pattern.matcher(inputPassword);
                if (!matcher.matches()) {
                    rules.add("至少包含一个大写字母（A-Z）");
                }

            }
            if ((value & 8) == 8) {
                sum += 8;
                regex = ".*\\d+.*";
                pattern = Pattern.compile(regex);
                matcher = pattern.matcher(inputPassword);
                if (!matcher.matches()) {
                    rules.add("至少包含一个数字（0－9）");
                }

            }
            if ((value & 16) == 16) {
                sum += 16;
                regex = ".*[\\p{Punct}&&[^'\"]]+.*";
                Pattern pattern1 = Pattern.compile(regex);
                Matcher matcher1 = pattern1.matcher(inputPassword);
                if(!matcher1.matches()){
                    rules.add("至少包含一个标点符号（英文输入法状态下，除“和空格外的所有符号）");
                }
            }
        }
        return rules.size()>0? String.join(", ", rules):"true";
    }
    public static void main(String[] args) {
        int value = 4;
        String rules = DMUserPasswordPolicy.validatePolicyRules(value, "@sA这是@一个测试句子！","Usernaem",9);
        System.out.println("Password Policy Rules for value " + value + ":");
            System.out.println(rules);
    }

}
