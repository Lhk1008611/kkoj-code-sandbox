package com.lhk.kkojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import com.lhk.kkojcodesandbox.model.ExecuteMessage;
import com.lhk.kkojcodesandbox.model.JudgeInfo;
import com.lhk.kkojcodesandbox.utils.ProcessUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JavaDockerCodeSandBox implements CodeSandBox {

    private final DockerClient dockerClient;

    private static final String TEMP_CODE_DIR_NAME = "tempCode";
    private static final String TEMP_FILE_NAME = "Main.java";
    private static final String DK_IMAGE_NAME = "openjdk:8u92-jdk-alpine";
    private static boolean IS_FIRST_JDK_IMAGE_PULL = true;
    private static boolean IS_RUN_TIME_OUT = true;

    public JavaDockerCodeSandBox(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

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
        try {
            ProcessUtils.run(compileCmd, "编译");
        } catch (IOException | InterruptedException e) {
            return getErrorResponse(e);
        }

        // 3. 把编译好的文件上传到容器环境内
        // 拉取 jdk 镜像，如果是项目第一次运行则拉取镜像
        if (IS_FIRST_JDK_IMAGE_PULL) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(DK_IMAGE_NAME);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("拉取 jdk 镜像" + item.getStatus());
                    super.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("拉取 jdk 镜像失败");
                    super.onError(throwable);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取 jdk 镜像失败");
                throw new RuntimeException(e);
            }
            System.out.println("拉取 jdk 镜像完成");
            IS_FIRST_JDK_IMAGE_PULL = false;
        }

        // 创建 jdk 容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(DK_IMAGE_NAME);
        HostConfig hostConfig = new HostConfig();
        // 容器内存限制、容器 cpu 限制
        hostConfig.withMemory(100 * 1024 * 1024L).withCpuCount(2L);
        // 数据卷挂载
        hostConfig.setBinds(new Bind(userCodeDIR, new Volume("/sandbox")));
        CreateContainerResponse containerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true) // 禁用网络资源
                .withReadonlyRootfs(true) //只读根目录下的资源
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)
                .withCmd("/bin/sh")
                .exec();
        String containerId = containerResponse.getId();
        System.out.println("创建 jdk 容器成功，id=" + containerId);
        //启动容器
        dockerClient.startContainerCmd(containerId).exec();

        List<ExecuteMessage> execMessageList = new ArrayList<>();

        // 4. 在容器中运行编译好的代码,执行多个输入用例得到多个输入用例的执行信息
        for (String input : inputList) {
            //构造命令
            String[] inputArgs = StringUtils.split(input, " ");
            String[] javaCmdArr = ArrayUtil.append(new String[]{"java", "-cp", "/sandbox", "Main"}, inputArgs);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(javaCmdArr)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("已创建程序运行命令" + execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();
            ExecuteMessage executeMessage = new ExecuteMessage();
            StopWatch stopWatch = new StopWatch();
            ExecStartResultCallback execStartResultCallback = getExecStartResultCallback(executeMessage);
            // 获取占用内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            // 执行 statsCmd 会异步的执行一个回调函数，该回调函数会一直在后台运行，监控容器的状态
            ResultCallback<Statistics> statsResultCallback = getResultCallback(executeMessage);
            statsCmd.exec(statsResultCallback);

            try {
                stopWatch.start();
                //运行命令
                System.out.println("运行代码");
                // 当程序运行正常切不超时，则会执行回调函数中的 onComplete() 方法，可以在此方法中判断是否是程序超时
                dockerClient.
                        execStartCmd(execId).
                        exec(execStartResultCallback).
                        awaitCompletion(ProcessUtils.TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                executeMessage.setExecTime(stopWatch.getLastTaskTimeMillis());
                statsCmd.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            execMessageList.add(executeMessage);
        }

        System.out.println("关闭容器");
        dockerClient.stopContainerCmd(containerId).exec();

        System.out.println("执行结果信息" + execMessageList);

        System.out.println("删除容器");
        dockerClient.removeContainerCmd(containerId).exec();

        // 5. 整理返回结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxExecTime = 0;
        long maxExecMemory = 0;
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
            Long execMemory = executeMessage.getExecMemory();
            if (execMemory != null){
                // 最大执行消耗内存
                maxExecMemory = Math.max(maxExecMemory, execMemory);
            }
        }
        // 结果正确设置状态为 1
        if (outputList.size() == execMessageList.size()){
            executeCodeResponse.setStatus(1);
            executeCodeResponse.setMessage("代码沙箱执行成功");
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(maxExecMemory);
        judgeInfo.setTime(maxExecTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        //6. 文件清理
        if (FileUtil.exist(userCodeFile.getParentFile())){
            boolean del = FileUtil.del(userCodeDIR);
            System.out.println(del ? "文件删除成功" : "文件删除失败");
        }
        return executeCodeResponse;
    }

    /**
     * 构造 ExecStartResultCallback 回调函数
     *
     * @param executeMessage
     * @return
     */

    private static ExecStartResultCallback getExecStartResultCallback(ExecuteMessage executeMessage) {
        final String[] message = {""};
        ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
            @Override
            public void onComplete() {
                IS_RUN_TIME_OUT = false;
            }

            @Override
            public void onNext(Frame frame) {
                message[0] = new String(frame.getPayload());
                if (StreamType.STDERR.equals(frame.getStreamType())) {
                    executeMessage.setErrorMessage(message[0]);
                    System.out.println("输出错误：" + message[0]);
                } else {
                    executeMessage.setSuccessMessage(message[0]);
                    System.out.println("输出结果：" + message[0]);
                }
                super.onNext(frame);
            }
        };
        return execStartResultCallback;
    }

    /**
     * 构造 ResultCallback 回调
     *
     * @param executeMessage
     * @return
     */
    private static ResultCallback<Statistics> getResultCallback(ExecuteMessage executeMessage) {

        final Long[] usage = {0l};
        return new ResultCallback<Statistics>() {
            @Override
            public void close() throws IOException {

            }

            @Override
            public void onStart(Closeable closeable) {
            }

            @Override
            public void onNext(Statistics statistics) {
                usage[0] = Math.max(statistics.getMemoryStats().getUsage(), usage[0]);
//                System.out.println("消耗内存:" + usage[0]);
                executeMessage.setExecMemory(usage[0]);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        };
    }

    /**
     * 执行代码沙箱出现异常时的错误处理
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Exception e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 代码沙箱出现异常, 设置状态为 2
        executeCodeResponse.setStatus(2);
        return executeCodeResponse;
    }

}
