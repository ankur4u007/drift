package com.ank.websockethttptunnel.client.service

import com.ank.websockethttptunnel.client.integration.http.WebHttpService
import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.common.util.sendAsyncBinaryData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler
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
                val localServerResponse = webHttpService.getResponseFromLocalServer(gossip.payload).map { responsePayload ->
                    session.sendAsyncBinaryData(gossip.copy(payload = responsePayload, event = Event.CLIENT_RESPOND),
                            clientRequestElasticScheduler, log, this::handleWebSocketRequest.name)
                }.subscribeOn(clientRequestElasticScheduler).publishOn(clientRequestElasticScheduler)
                        .doOnComplete {
                            session.sendAsyncBinaryData(gossip.copy(payload = gossip.payload?.copy(end = true), event = Event.CLIENT_RESPOND_END),
                                    clientRequestElasticScheduler, log, this::handleWebSocketRequest.name)
                        }.subscribe()
                clientCacheService.saveResponses(gossip.requestId.orEmpty(), localServerResponse)
            }
            Event.SERVER_REQUEST_ACK -> {
                clientCacheService.acknowledgeResponse(gossip.requestId.orEmpty())
            }
            else -> {}
        }
    }
}