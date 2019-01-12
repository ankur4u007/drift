package com.ank.drift.client.config

import com.ank.drift.common.contants.TEN_SECONDS
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tunnel.client")
data class ClientConfig(
    var key: String? = null,
    var remoteServer: RemoteServer? = null,
    var localServer: LocalServer? = null
)

data class RemoteServer(
    var url: String? = null,
    var key: String? = null,
    var ping: PingServer? = null
)

data class PingServer(
    var durationInSec: Long? = TEN_SECONDS,
    var delayInSec: Long? = TEN_SECONDS,
    var reconnectAfterMaxMisses: Long? = TEN_SECONDS
)

data class LocalServer(
    var url: String? = null,
    var connectTimeoutInSec: Long? = TEN_SECONDS,
    var readTimeoutInSec: Long? = TEN_SECONDS
)