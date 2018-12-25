package com.ank.websockethttptunnel.client.service

import com.ank.websockethttptunnel.client.integration.http.WebHttpService
import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.util.writeValueAsString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import javax.inject.Inject

@Service
class ClientEventHandlerService @Inject constructor(val clientCacheService: ClientCacheService,
                                                    val webHttpService: WebHttpService) {
    companion object {
        val log = LoggerFactory.getLogger(ClientEventHandlerService::class.java)
    }

    fun handle(session: WebSocketSession, gossip: Gossip): Mono<Void>{
        log.info("${ClientEventHandlerService::handle.name}, gossip=$gossip")
        return when(gossip.event) {
            Event.SERVER_PONG -> {
                clientCacheService.markForPong()
                Mono.empty()
            }
            Event.SERVER_REQUEST -> {
                clientCacheService.markForPong()
                webHttpService.getResponseFromLocalServer(gossip.payload).map { responsePayload ->
                    session.send(session
                            .textMessage(gossip.copy(payload = responsePayload, event = Event.CLIENT_RESPOND).writeValueAsString()).toMono()).doOnError {
                        log.error("${ClientEventHandlerService::handle.name}, Error=${it.message}", it)
                    }.subscribe()
                    responsePayload
                }.then()
            }
            else -> Mono.empty()
        }
    }
}