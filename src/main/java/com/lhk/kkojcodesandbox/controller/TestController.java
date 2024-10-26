package com.lhk.kkojcodesandbox.controller;


import cn.hutool.core.io.resource.ResourceUtil;
import com.github.dockerjava.api.DockerClient;
import com.lhk.kkojcodesandbox.DockerCodeSandBox;
import com.lhk.kkojcodesandbox.JavaDockerCodeSandBox;
import com.lhk.kkojcodesandbox.JavaNativeCodeSandBox;
import com.lhk.kkojcodesandbox.NativeCodeSandBox;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private DockerClient DockerJavaConfig;

    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    @Resource
    private NativeCodeSandBox nativeCodeSandBox;

    @GetMapping("/check")
    public String checkHealth() {
        return "ok";
    }

    @GetMapping("/dockerCodeSandbox1")
    public boolean test() {
        JavaDockerCodeSandBox javaDockerCodeSandBox = new JavaDockerCodeSandBox(DockerJavaConfig);
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4", "1 2", "5 5"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        return true;
    }

    @GetMapping("/dockerCodeSandbox2")
    public boolean test2() {
        DockerCodeSandBox javaDockerCodeSandBox = new DockerCodeSandBox(DockerJavaConfig);
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4", "1 2", "5 5"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        return true;
    }

    @GetMapping("/nativeCodeSandbox1")
    public boolean test3() {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4", "1 2", "5 5"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        return true;
    }

    @GetMapping("/nativeCodeSandbox4")
    public boolean test4() {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4", "1 2", "5 5"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = nativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        return true;
    }
}
