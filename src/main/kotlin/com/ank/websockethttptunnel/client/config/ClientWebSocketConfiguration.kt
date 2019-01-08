package com.ank.websockethttptunnel.client.config

import com.ank.websockethttptunnel.client.integration.ws.ClientWebSocketService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.Disposable
import javax.inject.Inject

@Configuration
class ClientWebSocketConfiguration @Inject constructor(val clientWebSocketService: ClientWebSocketService) {

    @Bean
    fun webSocketClient(): Disposable {
        return clientWebSocketService.getWebSocketClient()
    }
}