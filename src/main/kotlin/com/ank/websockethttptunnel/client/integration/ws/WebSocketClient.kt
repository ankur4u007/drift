package com.ank.websockethttptunnel.client.integration.ws

import com.ank.websockethttptunnel.client.service.ClientCacheService
import com.ank.websockethttptunnel.client.service.ClientEventHandlerService
import com.ank.websockethttptunnel.client.config.ClientConfig
import com.ank.websockethttptunnel.client.config.DELAY_IN_SEC
import com.ank.websockethttptunnel.client.config.DURATION_IN_SEC
import com.ank.websockethttptunnel.client.exception.ServerNotRespondingException
import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.util.parseToType
import com.ank.websockethttptunnel.common.util.writeValueAsString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.ConnectException
import java.net.URI
import java.time.Duration
import javax.inject.Inject

@Service
class WebSocketClientService @Inject constructor(val clientConfig: ClientConfig,
                                                 val clientEventHandlerService: ClientEventHandlerService,
                                                 val clientCacheService: ClientCacheService) {
    companion object {
        val log = LoggerFactory.getLogger(WebSocketClientService::class.java)
        val activeSession: WebSocketSession? = null
    }

    fun getWSClient() :  Disposable{
        val url = if (clientConfig.remoteServer?.url?.endsWith("/") == true) clientConfig.remoteServer?.url else clientConfig.remoteServer?.url.plus("/")
        val fullUrl = URI.create(clientConfig.remoteServer?.url).resolve("/websocket?key=${clientConfig.remoteServer?.key}")
        return ReactorNettyWebSocketClient().execute(fullUrl, connectWs())
                .doOnError {
                    log.error(it.message, it)
                }.retry {
                    log.info("${WebSocketClientService::getWSClient}, retrying because of ${it.message}")
                    it is ServerNotRespondingException || it is ConnectException
                }.subscribe()
    }

    private fun connectWs(): (WebSocketSession) -> Mono<Void> {
        return { session ->
            val response = session.receive().flatMap {
                it.payloadAsText.parseToType(Gossip::class.java).flatMap {
                    clientEventHandlerService.handle(it)
                }
            }.then()
            val request = session.send(
                    ping().flatMap {
                        if (clientCacheService.updateAndCheckPingStatus()) {
                            Flux.error(ServerNotRespondingException(Gossip(message = "Server missed ${clientConfig.remoteServer?.ping?.reconnectAfterMaxMisses} pings")))
                        } else {
                            Flux.just(it)
                        }
                    }.map { session.textMessage(Gossip(event = Event.CLIENT_PING).writeValueAsString()) }
            )
            Mono.zip(response, request).then()
        }
    }

    fun ping(): Flux<Long> {
        return Flux.interval(Duration.ofSeconds(clientConfig.remoteServer?.ping?.delayInSec ?: DELAY_IN_SEC),
                Duration.ofSeconds(clientConfig.remoteServer?.ping?.durationInSec ?: DURATION_IN_SEC))
    }
}