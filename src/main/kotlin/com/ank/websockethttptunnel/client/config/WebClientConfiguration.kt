package com.ank.websockethttptunnel.client.config

import com.ank.websockethttptunnel.client.integration.ws.WebSocketClientService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.Disposable
import javax.inject.Inject

@Configuration
class WebClientConfiguration @Inject constructor(val webSocketClientService: WebSocketClientService) {

    @Bean
    @ConditionalOnProperty(name = ["tunnel.client.enabled"], havingValue = "true")
    fun wsClient(): Disposable {
       return webSocketClientService.getWSClient()
    }
}