package com.project.server.springboot.configuration;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket configuration for Socket.IO server
 * Compatible with socket.io-client v4.x
 */
@Configuration
public class WebSocketConfig {

    @Value("${socketio.host:localhost}")
    private String host;

    @Value("${socketio.port:8080}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(10000);
        config.setPingTimeout(60000);
        config.setPingInterval(25000);
        config.setMaxHttpContentLength(1024 * 1024); // 1MB
        config.setBossThreads(1);
        config.setWorkerThreads(100);
        
        // CORS configuration - allow all origins in development
        // In production, specify your frontend URL
        config.setOrigin("*");
        
        return new SocketIOServer(config);
    }
}

