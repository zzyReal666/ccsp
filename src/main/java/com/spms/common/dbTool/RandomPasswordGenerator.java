package com.spms.common.dbTool;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

public class RandomPasswordGenerator {
    public static void main(String[] args) {
        int length = 16;

        AtomicReference<String> password = new AtomicReference<>(generateRandomPassword(length));
        System.out.println("Generated random password: " + password);
        for (int i = 0; i < 100; i++) {
            new Thread(()->{
                password.set(generateRandomPassword(length));
                System.out.println("Generated random password: " + password);
            }).start();
        }
    }

    /**
     * Generates a random password 必须有一个大写字母一个小写字母一个数字
     * @param length
     * @return
     */
    public static String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder();
        String lowerCaseChars = "abcdefghijklmnopqrstuvwxyz";
        String upperCaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digitChars = "0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder();
        //生成一个大写字母一个小写字母一个数字
        result.append(lowerCaseChars.charAt(random.nextInt(lowerCaseChars.length())));
        result.append(upperCaseChars.charAt(random.nextInt(upperCaseChars.length())));
        result.append(digitChars.charAt(random.nextInt(digitChars.length())));

        String allChars = lowerCaseChars + upperCaseChars + digitChars;
        // 生成剩余的字符
        while (result.length() < length) {
            result.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        // 随机排列结果中的字符
        char[] passwordArray = result.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int randomIndex = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }

        return new String(passwordArray);
    }
}
