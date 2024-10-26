package com.lhk.kkojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import com.lhk.kkojcodesandbox.model.ExecuteMessage;
import com.lhk.kkojcodesandbox.model.JudgeInfo;
import com.lhk.kkojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 使用模板方法对多个代码沙箱实现进行优化
 */
@Slf4j
public class CodeSandBoxTemplate implements CodeSandBox{

    private static final String TEMP_CODE_DIR_NAME = "tempCode";
    private static final String TEMP_FILE_NAME = "Main.java";
    private static final String SECURITY_MANAGER_DIR = File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"security";
    private static final String SECURITY_MANAGER_NAME = "MySecurityManager";

    /**
     * 命令黑名单
     */
    private static List<String> blackList = Arrays.asList("rm", "sh", "bash", "Files",  "System.getProperty", "exec");

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        File userCodeFile = saveCodeFile(code);

        ExecuteMessage compileMessage = compileCodeFile(userCodeFile);
        System.out.println("编译结果" + compileMessage);

        List<String> inputList = executeCodeRequest.getInputList();
        List<ExecuteMessage> execMessageList = executeCodeFile(userCodeFile, inputList);

        ExecuteCodeResponse executeCodeResponse = getExecuteCodeResponse(execMessageList);

        boolean isDelete = deleteCodeFile(userCodeFile);
        if (!isDelete){
            log.error("文件删除失败,路径 {}", userCodeFile.getPath());
        }
        return executeCodeResponse;
    }

    /**
     * 将代码保存为文件
     * @param code
     * @return
     */
    public File saveCodeFile(String code) {
        // 校验代码是否符合要求
        if (blackList.stream().anyMatch(subStr -> code.contains(subStr))){
            throw new RuntimeException("代码含有非法字符");
        }
        //1. 把用户的代码保存为文件
        String userDir = System.getProperty("user.dir");
        // 创建一个临时目录
        String tempCodeDir = userDir + File.separator + TEMP_CODE_DIR_NAME;
        if (!FileUtil.exist(tempCodeDir)) {
            FileUtil.mkdir(tempCodeDir);
        }
        // 将代码隔离存放，每个用户一个目录
        String userCodeDIR = tempCodeDir + File.separator + UUID.randomUUID();
        String userCodeFilePath = userCodeDIR + File.separator + TEMP_FILE_NAME;
        // 将代码写入文件
        File userCodeFile = FileUtil.writeString(code, userCodeFilePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 编译代码
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileCodeFile(File userCodeFile){
        String userCodeFilePath = userCodeFile.getPath();
        //2. 编译代码，得到 class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFilePath);
        try {
            ExecuteMessage executeMessage = ProcessUtils.run(compileCmd, "编译");
            if (executeMessage.getExitCode() != 0){
                throw new RuntimeException("代码编译错误");
            }
            return executeMessage;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行代码
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> executeCodeFile(File userCodeFile, List<String> inputList){
        String userCodeDIR = userCodeFile.getParent();
        //3. 执行代码，得到输出结果
        List<ExecuteMessage> execMessageList = new ArrayList<>();
        for (String inputArgs : inputList){
            String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeDIR, inputArgs);
            // 配置 java 安全管理器命令
//            String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeDIR, userDir+SECURITY_MANAGER_DIR, SECURITY_MANAGER_NAME, inputArgs);
            ExecuteMessage executeMessage = null;
            try {
                executeMessage = ProcessUtils.run(execCmd, "执行");
                execMessageList.add(executeMessage);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return execMessageList;
    }

    /**
     * 根据执行结果列表，生成执行结果
     * @param execMessageList
     * @return
     */
    public ExecuteCodeResponse getExecuteCodeResponse(List<ExecuteMessage> execMessageList){
        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxExecTime = 0;
        for (ExecuteMessage executeMessage : execMessageList){
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)){
                // 代码运行出现错误，设置状态为 3
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getSuccessMessage());
            Long execTime = executeMessage.getExecTime();
            if (execTime != null){
                // 最大执行时间
                maxExecTime = Math.max(maxExecTime, execTime);
            }
        }
        // 结果正确设置状态为 1
        if (outputList.size() == execMessageList.size()){
            executeCodeResponse.setStatus(1);
            executeCodeResponse.setMessage("代码沙箱执行成功");
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        // java 原生实现不做内存限制实现，只统计执行时间
        judgeInfo.setTime(maxExecTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除临时的代码文件
     *
     * @param userCodeFile
     * @return
     */
    public boolean deleteCodeFile(File userCodeFile){
        String userCodeDIR = userCodeFile.getParent();
        //5. 文件清理
        if (FileUtil.exist(userCodeFile.getParentFile())){
            boolean del = FileUtil.del(userCodeDIR);
            System.out.println(del ? "文件删除成功" : "文件删除失败");
            return del;
        }
        return true;
    }

    /**
     * 执行代码沙箱出现异常时的错误处理
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Exception e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 代码沙箱出现异常, 设置状态为 2
        executeCodeResponse.setStatus(2);
        return executeCodeResponse;
    }

}
