package com.lhk.kkojcodesandbox.controller;


import com.lhk.kkojcodesandbox.JavaNativeCodeSandBox;
import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/")
public class MainController {

    private static final String AUTH_REQUEST_HEADER = "apiAuth";
    private static final String AUTH_REQUEST_SECRET = "secret";


    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    /**
     * 调用代码沙箱接口
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeJavaCode")
    public ExecuteCodeResponse executeJavaCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        String authSecret = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return new ExecuteCodeResponse(null, "auth failed", 0, null);
        }
        return javaNativeCodeSandBox.executeCode(executeCodeRequest);
    }

}
