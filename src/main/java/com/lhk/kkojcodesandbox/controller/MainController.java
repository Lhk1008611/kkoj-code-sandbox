package com.lhk.kkojcodesandbox.controller;


import cn.hutool.core.io.resource.ResourceUtil;
import com.github.dockerjava.api.DockerClient;
import com.lhk.kkojcodesandbox.JavaDockerCodeSandBox;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController("/")
public class MainController {


    @Autowired
    private DockerClient DockerJavaConfig;

    @GetMapping("/check")
    public String checkHealth() {
        return "ok";
    }

    @GetMapping("/test")
    public boolean test(){
        JavaDockerCodeSandBox javaDockerCodeSandBox = new JavaDockerCodeSandBox(DockerJavaConfig);
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4","1 2","5 5"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        return true;
    }

}
