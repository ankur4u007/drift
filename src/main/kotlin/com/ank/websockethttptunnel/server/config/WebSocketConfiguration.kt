package com.ank.websockethttptunnel.server.config

import com.ank.websockethttptunnel.server.transport.ws.handlers.WebSockethandlers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory
import javax.inject.Inject

@Configuration
open class WebSocketConfiguration @Inject constructor(val webSocketHandler: WebSockethandlers) {

    @Bean
    fun handlerMapping(): HandlerMapping {
        val simpleUrlHandlerMapping = SimpleUrlHandlerMapping()
        simpleUrlHandlerMapping.urlMap = webSocketHandler.getAllwebSockethandlers()
        simpleUrlHandlerMapping.order = -1
        return simpleUrlHandlerMapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}