package com.lhk.kkojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import com.lhk.kkojcodesandbox.model.ExecuteMessage;
import com.lhk.kkojcodesandbox.model.JudgeInfo;
import com.lhk.kkojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandBox implements CodeSandBox {

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
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 校验代码是否符合要求
        if (blackList.stream().anyMatch(subStr -> code.contains(subStr))){
            return new ExecuteCodeResponse(new ArrayList<>(), "代码含有非法词汇", 2, null);
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

        //2. 编译代码，得到 class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFilePath);
        ExecuteMessage compileMessage = null;
        try {
            compileMessage = ProcessUtils.run(compileCmd, "编译");
        } catch (IOException|InterruptedException e) {
            return getErrorResponse(e);
        }

        //3. 执行代码，得到输出结果
        List<ExecuteMessage> execMessageList = new ArrayList<>();
        for (String inputArgs : inputList){
            String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeDIR, inputArgs);
//            String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeDIR, userDir+SECURITY_MANAGER_DIR, SECURITY_MANAGER_NAME, inputArgs);

            ExecuteMessage executeMessage = null;
            try {
                executeMessage = ProcessUtils.run(execCmd, "执行");
                execMessageList.add(executeMessage);
            } catch (IOException | InterruptedException e) {
                return getErrorResponse(e);
            }
        }

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
        // todo 获取内存消耗
//        judgeInfo.setMemory();
        judgeInfo.setTime(maxExecTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        //5. 文件清理
        if (FileUtil.exist(userCodeFile.getParentFile())){
            boolean del = FileUtil.del(userCodeDIR);
            System.out.println(del ? "文件删除成功" : "文件删除失败");
        }
        //6. 错误处理，提升程序健壮性
        return executeCodeResponse;
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
