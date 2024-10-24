package com.lhk.kkojcodesandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.lhk.kkojcodesandbox.config.DockerJavaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;

@SpringBootApplication
@Import(DockerJavaConfig.class)
public class KkojCodeSandboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(KkojCodeSandboxApplication.class, args);
    }

}
