package com.ank.websockethttptunnel.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

const val DURATION_IN_SEC: Long = 10
const val DELAY_IN_SEC: Long = 10
const val MAX_PING_MISSES: Int = 10

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
        var durationInSec: Long? = DURATION_IN_SEC,
        var delayInSec: Long? = DELAY_IN_SEC,
        var maxMisses: Int? = MAX_PING_MISSES
)