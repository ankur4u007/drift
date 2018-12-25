package com.ank.websockethttptunnel.client.config

import com.ank.websockethttptunnel.client.integration.ws.WebSocketClientService
import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.Disposable
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import java.time.Duration
import javax.inject.Inject

@Configuration
@ConditionalOnProperty(name = ["tunnel.client.enabled"], havingValue = "true")
class WebClientConfiguration @Inject constructor(val webSocketClientService: WebSocketClientService,
                                                 val clientConfig: ClientConfig) {

    @Bean
    fun wsClient(): Disposable {
       return webSocketClientService.getWSClient()
    }

}