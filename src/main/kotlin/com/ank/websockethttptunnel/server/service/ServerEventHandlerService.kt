package com.ank.websockethttptunnel.server.service

import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.server.exception.BadRequestException
import io.netty.handler.codec.http.HttpResponseStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import javax.inject.Inject

@Service
class ServerEventHandlerService @Inject constructor(val sessionCacheService: SessionCacheService) {

    companion object {
        val log = LoggerFactory.getLogger(ServerEventHandlerService::class.java)
    }
    fun handle(gossip: Gossip, sessionId: String): Mono<Gossip> {
        log.info("${ServerEventHandlerService::handle.name}, Server Received=$gossip, sessionId=$sessionId")
        return when (gossip.event) {
            Event.CLIENT_PING -> {
                sessionCacheService.updateClientTimestamp(sessionId)
                Mono.just(Gossip(event = Event.SERVER_PONG, status = HttpResponseStatus.OK.code()))
            }
            Event.CLIENT_RESPOND -> {
                sessionCacheService.updateClientTimestamp(sessionId)
                sessionCacheService.savePayload(gossip)
                Mono.just(Gossip(requestId = gossip.requestId, event = Event.SERVER_REQUEST_ACK, status = HttpResponseStatus.OK.code()))
            }
            else -> Mono.error(BadRequestException(Gossip(status = HttpResponseStatus.BAD_REQUEST.code(), message = "event:${gossip.event} not supported")))
        }
    }
}