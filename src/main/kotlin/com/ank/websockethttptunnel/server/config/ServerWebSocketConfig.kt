package com.ank.websockethttptunnel.server.config

import com.ank.websockethttptunnel.server.transport.ws.ServerWebSocketRequestHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import javax.inject.Inject

@Configuration
class ServerWebSocketConfig @Inject constructor(
    val registrationHandler: ServerWebSocketRequestHandler
) {

    @Bean
    fun registerSocketMapping(webSockethandlers: Map<String, WebSocketHandler>): HandlerMapping {
        val simpleUrlHandlerMapping = SimpleUrlHandlerMapping()
        simpleUrlHandlerMapping.urlMap = hashMapOf<String, WebSocketHandler>(
                "/websocket" to registrationHandler
        )
        simpleUrlHandlerMapping.order = -1
        return simpleUrlHandlerMapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        val requestUpgradeStrategy = ReactorNettyRequestUpgradeStrategy()
        requestUpgradeStrategy.maxFramePayloadLength = 1024 * 1024 * 1024
        return WebSocketHandlerAdapter(HandshakeWebSocketService(requestUpgradeStrategy))
    }
}