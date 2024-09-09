package com.lhk.kkojcodesandbox.model;

import lombok.Data;

@Data
public class ExecuteMessage {
    /**
     * 退出码
     */
    private Integer exitCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行成功信息
     */
    private String successMessage;

    /**
     * 执行时间
     */
    private Long execTime;
}
