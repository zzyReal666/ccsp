package com.spms.common.shell;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/8/29 09:01
 */
@Slf4j
public class ShellScriptExecutor {

    /**
     * 执行Shell脚本并返回执行结果
     *
     * @param scriptPath Shell脚本的绝对路径
     * @param args       传递给Shell脚本的参数
     * @return 执行结果，包括输出和退出状态
     */
    public static ExecutionResult executeScript(String scriptPath, String... args) {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add(scriptPath);
        // 添加脚本参数
        command.addAll(Arrays.asList(args));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // 合并标准输出和错误输出
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            // 获取脚本输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            // 等待脚本执行完成
            int exitCode = process.waitFor();
            return new ExecutionResult(exitCode, output.toString());
        } catch (IOException | InterruptedException e) {
            log.error("执行Shell脚本失败,脚本路径:{},参数:{}", scriptPath, Arrays.toString(args), e);
            return new ExecutionResult(-1, e.getMessage());
        }
    }

    /**
     * Shell脚本执行结果
     */
    @Getter
    public static class ExecutionResult {
        private final int exitCode;
        private final String output;

        public ExecutionResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        @Override
        public String toString() {
            return "ExecutionResult{" + "exitCode=" + exitCode + ", output='" + output + '\'' + '}';
        }
    }
}