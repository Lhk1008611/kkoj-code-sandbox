package com.lhk.kkojcodesandbox;


import com.lhk.kkojcodesandbox.model.ExecuteCodeRequest;
import com.lhk.kkojcodesandbox.model.ExecuteCodeResponse;

/**
 * @description: 代码沙箱接口
 */
public interface CodeSandBox {
     /**
      * 执行代码
      * @param executeCodeRequest
      * @return ExecuteCodeResponse
      */
     ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
