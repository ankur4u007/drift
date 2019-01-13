package com.ank.drift.client.service

import com.ank.drift.client.integration.http.ClientWebHttpService
import com.ank.drift.common.model.Event
import com.ank.drift.common.model.Gossip
import com.ank.drift.common.util.sendAsyncBinaryData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.scheduler.Scheduler
import javax.inject.Inject

@Service
class ClientEventHandlerService @Inject constructor(
    val clientCacheService: ClientCacheService,
    val clientWebHttpService: ClientWebHttpService,
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
                val responseDisposable = clientWebHttpService.getResponseFromLocalServer(gossip.payload)
                        .buffer(1000)
                        .map {
                            val reducedPayload = it.reduce { accumulatedPayload, payload ->
                                val temp = ByteArray(accumulatedPayload.body.size + payload.body.size)
                                System.arraycopy(accumulatedPayload.body, 0, temp, 0, accumulatedPayload.body.size)
                                System.arraycopy(payload.body, 0, temp, accumulatedPayload.body.size, payload.body.size)
                                accumulatedPayload.copy(body = temp)
                            }
                            session.sendAsyncBinaryData(gossip.copy(payload = reducedPayload, event = Event.CLIENT_RESPOND),
                                    clientRequestElasticScheduler, log, this::handleWebSocketRequest.name)
                        }.subscribeOn(clientRequestElasticScheduler)
                        .publishOn(clientRequestElasticScheduler)
                        .doOnComplete {
                            session.sendAsyncBinaryData(gossip.copy(payload = gossip.payload?.copy(end = true), event = Event.CLIENT_RESPOND_END),
                                    clientRequestElasticScheduler, log, this::handleWebSocketRequest.name)
                        }.subscribe()
                clientCacheService.saveResponses(gossip.requestId.orEmpty(), responseDisposable)
            }
            Event.SERVER_REQUEST_ACK -> {
                clientCacheService.acknowledgeResponse(gossip.requestId.orEmpty())
            }
            else -> {}
        }
    }
}