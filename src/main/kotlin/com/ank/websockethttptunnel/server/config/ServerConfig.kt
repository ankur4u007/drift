package com.ank.websockethttptunnel.server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tunnel.server")
data class ServerConfig (
    var key: String? = null
)