package com.ank.websockethttptunnel.server.service

import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.common.util.writeValueAsString
import com.ank.websockethttptunnel.server.config.ServerConfig
import com.ank.websockethttptunnel.server.exception.ClientUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration
import java.util.UUID
import javax.inject.Inject

@Service
class WebHttpSocketDelegator @Inject constructor(val sessionCacheService: SessionCacheService,
                                                 val serverConfig: ServerConfig) {
    companion object {
        val log = LoggerFactory.getLogger(WebHttpSocketDelegator::class.java)
    }


    fun getResponse(payload: Payload): Mono<Payload> {
        val requestId = UUID.randomUUID().toString()
        val session = sessionCacheService.getClient()?.session
        session?.send(session.textMessage(Gossip(event = Event.SERVER_REQUEST, payload = payload, requestId = requestId).writeValueAsString())
                .toMono())?.subscribe()
        return Flux.interval(Duration.ofMillis(200))
                .flatMap { Mono.justOrEmpty(sessionCacheService.getPayload(requestId)) }
                .take(Duration.ofSeconds(serverConfig.remoteClient?.timeoutInSec ?: TEN_SECONDS))
                .doOnNext { log.info("${WebHttpSocketDelegator::getResponse.name}, Received response $it") }
                .filter { it != null }
                .switchIfEmpty(Mono.error(ClientUnavailableException()))
                .toMono()
    }
}