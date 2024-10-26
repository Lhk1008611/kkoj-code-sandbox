package com.lhk.kkojcodesandbox;

import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import com.lhk.kkojcodesandbox.model.ExecuteMessage;
import com.lhk.kkojcodesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DockerCodeSandBox extends CodeSandBoxTemplate{

    private final DockerClient dockerClient;
    private static final String DK_IMAGE_NAME = "openjdk:8u92-jdk-alpine";
    private static boolean IS_FIRST_JDK_IMAGE_PULL = true;
    private static boolean IS_RUN_TIME_OUT = true;

    public DockerCodeSandBox(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    /**
     * 使用docker执行代码
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> executeCodeFile(File userCodeFile, List<String> inputList) {
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
        String userCodeDIR = userCodeFile.getParentFile().getAbsolutePath();
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

        return execMessageList;
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
}
