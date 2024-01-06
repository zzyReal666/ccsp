package com.spms.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @project ccsp
 * @description DM列定義解析
 * @author 18853
 * @date 2024/1/4 15:41:28
 * @version 1.0
 */
public class ParseCreateSQL {

    public static String parseCreateSQL(String sql, boolean clean) {
        if (sql == null || sql.length() < 1) {
            return null;
        }

        sql = sql.trim();
        if(!sql.startsWith("CREATE") && !sql.startsWith("create")) {
            return null;
        }

        int iStartIndex = -1;
        int iEndIndex = -1;
        int iLeftCount = 0;
        int iRightCount = 0;
        for(int i = 0; i < sql.length(); i++) {
            if(sql.charAt(i) == '(') {
                if (iStartIndex < 0) {
                    iStartIndex = i;
                }
                iLeftCount++;

                continue;
            }

            if (sql.charAt(i) == ')') {
                iRightCount++;

                if (iRightCount == iLeftCount) {
                    iEndIndex = i;
                    break;
                }

                continue;
            }
        }

        sql = sql.substring(iStartIndex + 1, iEndIndex);
        if (clean) {
            String targetSQL = "";
            ByteArrayInputStream bs = new ByteArrayInputStream(sql.getBytes());
            InputStreamReader insr = new InputStreamReader(bs);
            BufferedReader br = new BufferedReader(insr);

            while(true) {
                try {
                    String perLine = br.readLine();
                    if(perLine == null) {
                        break;
                    }
                    perLine = perLine.trim();
                    if (perLine.length() < 1) {
                        continue;
                    }

                    if(perLine.startsWith("\"")) {
                        if (perLine.charAt(perLine.length() - 1) == ',') {
                            perLine = perLine.substring(0, perLine.length() - 1);
                        }
                        targetSQL = targetSQL + perLine + System.getProperty("line.separator");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                br.close();
                insr.close();
                bs.close();
            } catch(Exception e) {}

            sql = targetSQL;
        }

        return sql;
    }
}
