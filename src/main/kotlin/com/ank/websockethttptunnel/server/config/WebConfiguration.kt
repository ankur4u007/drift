package com.ank.websockethttptunnel.server.config

import com.ank.websockethttptunnel.common.contants.SIXTY_SECONDS
import com.ank.websockethttptunnel.common.util.JsonConverter
import com.ank.websockethttptunnel.server.service.SessionCacheService
import com.ank.websockethttptunnel.server.transport.http.WebHttpHandler
import com.ank.websockethttptunnel.server.transport.ws.WebSockethandlers
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.Disposable
import reactor.core.publisher.Flux
import java.time.Duration
import javax.inject.Inject

@Configuration
@EnableWebFlux
@ConditionalOnProperty(name = ["tunnel.server.enabled"], havingValue = "true")
class WebConfiguration @Inject constructor(val webSocketHandler: WebSockethandlers,
                                           val serverConfig: ServerConfig,
                                           val sessionCacheService: SessionCacheService,
                                           val webHttpHandler: WebHttpHandler) : WebFluxConfigurer {

    @Bean
    fun registerSocketMapping(): HandlerMapping {
        val simpleUrlHandlerMapping = SimpleUrlHandlerMapping()
        simpleUrlHandlerMapping.urlMap = webSocketHandler.getAllwebSockethandlers()
        simpleUrlHandlerMapping.order = -1
        return simpleUrlHandlerMapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

    @Bean
    fun startCacheEviction(): Disposable {
        return Flux.interval(Duration.ofSeconds(serverConfig.remoteClient?.evictDurationInSec ?: SIXTY_SECONDS))
                .map {
                    sessionCacheService.evictCache()
                }.retry {
                    it is Exception
                }.subscribe()
    }

    @Bean
    fun registerHttpMapping(): RouterFunction<ServerResponse> {
        return webHttpHandler.handle()
    }

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(JsonConverter.objectMapper
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)))

        configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(JsonConverter.objectMapper
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)))
    }

}