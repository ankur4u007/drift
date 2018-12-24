package com.ank.websockethttptunnel.server.config

import com.ank.websockethttptunnel.server.transport.ws.handlers.WebSockethandlers
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import javax.inject.Inject

@Configuration
class WebConfiguration @Inject constructor(val webSocketHandler: WebSockethandlers) {

    @Bean
    @ConditionalOnProperty(name = ["tunnel.server.enabled"], havingValue = "true")
    fun handlerMapping(): HandlerMapping {
        val simpleUrlHandlerMapping = SimpleUrlHandlerMapping()
        simpleUrlHandlerMapping.urlMap = webSocketHandler.getAllwebSockethandlers()
        simpleUrlHandlerMapping.order = -1
        return simpleUrlHandlerMapping
    }

    @Bean
    @ConditionalOnProperty(name = ["tunnel.server.enabled"], havingValue = "true")
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}