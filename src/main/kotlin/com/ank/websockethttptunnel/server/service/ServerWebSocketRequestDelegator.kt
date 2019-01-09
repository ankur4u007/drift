package com.ank.websockethttptunnel.server.service

import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.common.util.sendAsyncBinaryData
import com.ank.websockethttptunnel.server.config.ServerConfig
import com.ank.websockethttptunnel.server.exception.ClientTimeoutException
import com.ank.websockethttptunnel.server.exception.ClientUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeoutException
import javax.inject.Inject

@Service
class ServerWebSocketRequestDelegator @Inject constructor(
    val sessionCacheService: SessionCacheService,
    val serverConfig: ServerConfig,
    val requestElasticScheduler: Scheduler
) {
    companion object {
        val log = LoggerFactory.getLogger(ServerWebSocketRequestDelegator::class.java)
    }

    fun getResponse(payload: Payload): Flux<Payload> {
        val requestId = UUID.randomUUID().toString()
        return sessionCacheService.getClient()?.session?.let {
            it.sendAsyncBinaryData(Gossip(event = Event.SERVER_REQUEST, payload = payload, requestId = requestId),
                    requestElasticScheduler, log, this::getResponse.name)
            return Flux.interval(Duration.ofMillis(200))
                    .flatMap { sessionCacheService.getPayload(requestId)?.toMono() ?: Mono.empty() }
                    .takeUntil { payload -> payload.end }
                    .timeout(Duration.ofSeconds(serverConfig.remoteClient?.timeoutInSec ?: TEN_SECONDS))
                    .filter { it != null }
                    .onErrorMap {
                        if (it is TimeoutException) {
                            ClientTimeoutException()
                        } else {
                            it
                        }
                    }.doOnNext { log.info("${ServerWebSocketRequestDelegator::getResponse.name}, Received response $it") }
        } ?: Flux.error(ClientUnavailableException())
    }
}