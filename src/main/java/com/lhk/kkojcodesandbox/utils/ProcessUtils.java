package com.lhk.kkojcodesandbox.utils;

import com.lhk.kkojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ProcessUtils {

    private static final long TIME_OUT = 5000L;


    public static ExecuteMessage run(String command, String operationName) throws IOException, InterruptedException {

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            //执行命令
            Process runProcess = Runtime.getRuntime().exec(command);
            // 创建超时控制线程，如果超过5秒，则杀死 runProcess 进程
            new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("程序执行超时");
                        if (runProcess.isAlive()){
                            runProcess.destroy();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
            }).start();
            //等待命令执行完毕，得到程序执行码
            int exitCode = runProcess.waitFor();
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setExitCode(exitCode);
            if (exitCode == 0) {
                System.out.println(operationName + "成功");
                // 读取正常的输出
                String execOutput = readExecOutput(runProcess.getInputStream());
                executeMessage.setSuccessMessage(execOutput);
            } else {
                System.out.println(operationName + "失败，错误码 " + exitCode);
                // 读取正常的输出
                String execOutput = readExecOutput(runProcess.getInputStream());
                executeMessage.setSuccessMessage(execOutput);
                // 读取错误的输出
                String errorExecOutput = readExecOutput(runProcess.getErrorStream());
                executeMessage.setErrorMessage(errorExecOutput);
            }
            stopWatch.stop();
            executeMessage.setExecTime(stopWatch.getLastTaskTimeMillis());
            return executeMessage;
    }

    private static String readExecOutput(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String outputLine;
        StringBuilder result = new StringBuilder();
        try {
            while ((outputLine = bufferedReader.readLine()) != null) {
                result.append("\n").append(outputLine);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }
}
