package com.lhk.kkojcodesandbox.security;

import java.security.Permission;

public class MySecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {

    }

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("禁止执行外部命令：" + cmd);
    }
}
