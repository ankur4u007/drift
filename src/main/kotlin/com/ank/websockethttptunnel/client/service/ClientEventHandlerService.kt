package com.ank.websockethttptunnel.client.service

import com.ank.websockethttptunnel.client.integration.http.WebHttpService
import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.util.sendAsyncBinaryData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import javax.inject.Inject

@Service
class ClientEventHandlerService @Inject constructor(
    val clientCacheService: ClientCacheService,
    val webHttpService: WebHttpService,
    val clientRequestElasticScheduler: Scheduler
) {
    companion object {
        val log = LoggerFactory.getLogger(ClientEventHandlerService::class.java)
    }

    fun handleWebSocketRequest(session: WebSocketSession, gossip: Gossip) {
        log.info("${ClientEventHandlerService::handleWebSocketRequest.name}, Client Received=$gossip, sessionId=${session.id}")
        when (gossip.event) {
            Event.SERVER_PONG -> {
                clientCacheService.markForPong()
            }
            Event.SERVER_REQUEST -> {
                clientCacheService.markForPong()
                webHttpService.getResponseFromLocalServer(gossip.payload).map { responsePayload ->
                    session.sendAsyncBinaryData(gossip.copy(payload = responsePayload, event = Event.CLIENT_RESPOND),
                            clientRequestElasticScheduler, log, this::handleWebSocketRequest.name)
                }.subscribeOn(clientRequestElasticScheduler).publishOn(clientRequestElasticScheduler).subscribe()
            }
            else -> {}
        }
    }
}