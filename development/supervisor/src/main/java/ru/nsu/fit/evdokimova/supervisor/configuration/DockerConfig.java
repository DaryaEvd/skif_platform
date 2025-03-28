package ru.nsu.fit.evdokimova.supervisor.configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientImpl;
//import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import java.time.Duration;
import java.util.List;

@Configuration
public class DockerConfig {
//    @Value("${docker.socket.path}")
//    private String dockerSocketPath;

    @Bean
    public DockerClient buildDockerClient() {
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();


        return DockerClientImpl.getInstance(standard);
    }
}
//        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

////        DefaultDockerClientConfig.Builder dockerClientConfigBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
////
////        if(this.dockerSocketPath != null && this.dockerSocketPath.startsWith("unix://")) {
////            dockerClientConfigBuilder.withDockerHost(dockerSocketPath)
////                    .withDockerTlsVerify(false);
////        }
////
////        DefaultDockerClientConfig dockerClientConfig = dockerClientConfigBuilder
////                .build();
////        ApacheDockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
////                .dockerHost(dockerClientConfig.getDockerHost()).build();
////
////        return DockerClientBuilder.getInstance(dockerClientConfig)
////                .withDockerHttpClient(dockerHttpClient)
////                .build();
//        ///////////////////////////////////////////////////
//
//
//        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
////                .withDockerHost("tcp://192.168.31.156:2375")
////                .withDockerTlsVerify(false)
//                .build();
//
////        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
////        return dockerClient;
//
//
////        DefaultDockerClientConfig.Builder dockerClientConfigBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
////
////        if(this.dockerSocketPath != null&& this.dockerSocketPath.startsWith("unix://")) {
////            dockerClientConfigBuilder.withDockerHost(dockerSocketPath)
////                    .withDockerTlsVerify(false);
////        }
////        DefaultDockerClientConfig dockerClientConfig = dockerClientConfigBuilder
////                .build();
////
////        ApacheDockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
////                .dockerHost(dockerClientConfig.getDockerHost()).build();
////
////        return DockerClientBuilder.getInstance(dockerClientConfig)
////                .withDockerHttpClient(dockerHttpClient)
////                .build();
//
//        //                .withDockerHost("unix:///var/run/docker.sock")
////                .build();
//
//        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
//                .dockerHost(config.getDockerHost())
//                .sslConfig(config.getSSLConfig())
////                .maxConnections(100)
////                .connectionTimeout(Duration.ofSeconds(30))
////                .responseTimeout(Duration.ofSeconds(45))
//                .build();
////        return DockerClientImpl.getInstance(config, httpClient);
//
//        return DockerClientBuilder.getInstance(config)
//                .withDockerHttpClient(httpClient)
//                .build();
//    }
//}
