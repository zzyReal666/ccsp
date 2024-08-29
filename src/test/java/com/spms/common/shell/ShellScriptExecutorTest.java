package com.spms.common.shell;


import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


public class ShellScriptExecutorTest {

    // 定义脚本路径
    private static final String SCRIPT_PATH = "test_script.sh";  // 替换为实际路径



    @Test
    public void testExecuteScriptSuccess() {
        // 获取脚本路径（从 resources 目录中）
        URL resource = getClass().getClassLoader().getResource("test_script.sh");
        if (resource == null) {
            throw new IllegalArgumentException("Script file not found in resources");
        }
        String scriptPath = resource.getPath();

        // 准备测试参数
        String parameter = "World";

        // 执行脚本
        ShellScriptExecutor.ExecutionResult result = ShellScriptExecutor.executeScript(scriptPath, 30, parameter);

        // 验证脚本执行结果
        assertEquals(0, result.getExitCode(), "The script should exit with code 0.");
        assertTrue(result.getOutput().contains("Hello, World!"), "The output should contain 'Hello, World!'");
    }

    @Test
    public void testExecuteScriptFailure() {
        // 使用一个不存在的脚本路径来测试
        String invalidScriptPath = "/invalid/path/to/script.sh";

        // 执行脚本
        ShellScriptExecutor.ExecutionResult result = ShellScriptExecutor.executeScript(invalidScriptPath, 30);

        // 验证脚本执行结果
        assertNotEquals(0, result.getExitCode(), "The script should not exit with code 0 for an invalid path.");
        assertTrue(result.getOutput().contains("No such file or directory") || result.getOutput().contains("error"), "The output should indicate that the script was not found.");
    }

    @Test
    public void testExecuteScriptWithoutArguments() {
        // 执行不需要参数的脚本
        ShellScriptExecutor.ExecutionResult result = ShellScriptExecutor.executeScript(SCRIPT_PATH, 30);

        // 验证脚本执行结果
        assertEquals(0, result.getExitCode(), "The script should exit with code 0 when no arguments are provided.");
        assertTrue(result.getOutput().contains("Hello,"), "The output should contain 'Hello,' indicating no parameter.");
    }

    @Test
    public void testScriptPathValidity() {
        // 检查脚本路径是否存在
        Path scriptPath = Paths.get(SCRIPT_PATH); // 使用 Paths.get() 代替 Path.of()
        assertTrue(Files.exists(scriptPath), "The script path should exist.");
        assertTrue(Files.isExecutable(scriptPath), "The script should be executable.");
    }
}