package com.lhk.kkojcodesandbox.controller;


import com.lhk.kkojcodesandbox.JavaNativeCodeSandBox;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/")
public class MainController {

    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    /**
     * 调用代码沙箱接口
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeJavaCode")
    public ExecuteCodeResponse executeJavaCode(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        return javaNativeCodeSandBox.executeCode(executeCodeRequest);
    }

}
