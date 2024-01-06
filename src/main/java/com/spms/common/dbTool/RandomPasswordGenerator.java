package com.spms.common.dbTool;

import java.util.Random;

public class RandomPasswordGenerator {
    public static void main(String[] args) {
        int length = 16; // 设置密码长度为10位

        String password = generateRandomPassword(length);
        System.out.println("Generated random password: " + password);
    }

    public static String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder();
        char[] characters = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        for (char c : characters) {
            sb.append(c).append(",");
        }
        characters = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        for (char c : characters) {
            sb.append(c).append(",");
        }
        characters = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        for (char c : characters) {
            sb.append(c).append(",");
        }

        String characterSet = sb.toString().replaceAll(",$", "").trim();
         characterSet = characterSet.replaceAll(",", "").trim();
        System.out.println(sb.toString());
        Random rand = new Random();
        StringBuilder result = new StringBuilder();
        while (result.length() < length) {
            int index = rand.nextInt(characterSet.length());
            char nextChar = characterSet.charAt(index);

            if (!result.toString().contains(String.valueOf(nextChar)) ){
                result.append(nextChar);
            }
        }

        return result.toString();
    }


}
