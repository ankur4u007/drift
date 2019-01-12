package com.ank.drift.server.config

import com.ank.drift.common.contants.SIXTY_SECONDS
import com.ank.drift.common.contants.TEN_SECONDS
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tunnel.server")
data class ServerConfig(
    var key: String? = null,
    var remoteClient: RemoteClient? = null

)

data class RemoteClient(
    var evictDurationInSec: Long? = SIXTY_SECONDS,
    var timeoutInSec: Long? = TEN_SECONDS
)