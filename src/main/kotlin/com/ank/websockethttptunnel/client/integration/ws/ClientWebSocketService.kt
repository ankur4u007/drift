package com.ank.websockethttptunnel.client.integration.ws

import com.ank.websockethttptunnel.client.service.ClientCacheService
import com.ank.websockethttptunnel.client.service.ClientEventHandlerService
import com.ank.websockethttptunnel.client.config.ClientConfig
import com.ank.websockethttptunnel.client.exception.ServerNotRespondingException
import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.util.sendAsyncBinaryData
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.SerializationUtils
import org.springframework.util.StreamUtils
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler
import java.net.ConnectException
import java.net.URI
import java.time.Duration
import javax.inject.Inject

@Service
class ClientWebSocketService @Inject constructor(
    val clientConfig: ClientConfig,
    val clientEventHandlerService: ClientEventHandlerService,
    val clientCacheService: ClientCacheService,
    val clientRegistrationElasticScheduler: Scheduler,
    val clientRequestElasticScheduler: Scheduler,
    val clientPingElasticScheduler: Scheduler
) {
    companion object {
        val log = LoggerFactory.getLogger(ClientWebSocketService::class.java)
    }

    fun getWebSocketClient(): Disposable {
        val fullUrl = URI.create(clientConfig.remoteServer?.url).resolve("/websocket?key=${clientConfig.remoteServer?.key}")
        val reactorNettyRequestUpgradeStrategy = ReactorNettyRequestUpgradeStrategy()
        reactorNettyRequestUpgradeStrategy.maxFramePayloadLength = 1000000000

        return ReactorNettyWebSocketClient().execute(fullUrl, handleWebSocketSession())
                .doOnError {
                    log.error(it.message, it)
                }.retry {
                    log.info("${ClientWebSocketService::getWebSocketClient.name}, retrying because of ${it.message}")
                    it is ServerNotRespondingException || it is ConnectException || it is WebSocketHandshakeException
                }.subscribeOn(clientRegistrationElasticScheduler)
                .subscribe()
    }

    private fun handleWebSocketSession(): (WebSocketSession) -> Mono<Void> {
        return { session ->
            val request = session.receive().map {
                val gossip = SerializationUtils.deserialize(StreamUtils.copyToByteArray(it.payload.asInputStream())) as Gossip
                clientEventHandlerService.handleWebSocketRequest(session, gossip)
            }.then()

            val ping = ping().flatMap {
                if (clientCacheService.updateAndCheckPingStatus()) {
                    Mono.error(ServerNotRespondingException(Gossip(message = "Server missed ${clientConfig.remoteServer?.ping?.reconnectAfterMaxMisses} pings")))
                } else {
                    Mono.just(it)
                }
            }.map {
                session.sendAsyncBinaryData(Gossip(event = Event.CLIENT_PING), clientPingElasticScheduler, log, this::handleWebSocketSession.name)
            }.onErrorResume {
                Mono.error(ServerNotRespondingException(Gossip(message = it.message)))
            }.then()
            Flux.merge(request, ping).toMono()
        }
    }

    fun ping(): Flux<Long> {
        return Flux.interval(Duration.ofSeconds(clientConfig.remoteServer?.ping?.delayInSec ?: TEN_SECONDS),
                Duration.ofSeconds(clientConfig.remoteServer?.ping?.durationInSec ?: TEN_SECONDS))
    }
}