package com.ank.websockethttptunnel.client.config

import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tunnel.client")
data class ClientConfig (
        var key: String? = null,
        var remoteServer: RemoteServer? = null,
        var localServer: String? = null
)

data class RemoteServer (
        var url: String? = null,
        var key: String? = null,
        var ping: PingServer? = null
)

data class PingServer (
        var durationInSec: Long? = TEN_SECONDS,
        var delayInSec: Long? = TEN_SECONDS,
        var reconnectAfterMaxMisses: Long? = TEN_SECONDS
)