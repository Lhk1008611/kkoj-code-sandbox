package com.lhk.kkojcodesandbox;

import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * 使用模板方法设计模式优化 java 原生实现的代码沙箱
 */
@Component
public class NativeCodeSandBox extends CodeSandBoxTemplate{

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

}
