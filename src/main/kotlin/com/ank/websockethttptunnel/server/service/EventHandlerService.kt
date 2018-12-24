package com.ank.websockethttptunnel.server.service

import com.ank.websockethttptunnel.common.model.Event
import com.ank.websockethttptunnel.common.model.Gossip
import io.netty.handler.codec.http.HttpResponseStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import javax.inject.Inject

@Service
class EventHandlerService @Inject constructor(val sessionCacheService: SessionCacheService) {

    companion object {
        val log = LoggerFactory.getLogger(EventHandlerService::class.java)
    }
    fun handle(gossip: Gossip, sessionId: String): Mono<Gossip> {
        log.info("${EventHandlerService::handle.name}, gossip=$gossip")
        return Mono.just(when(gossip.event) {
            Event.CLIENT_PING -> {
                sessionCacheService.updateClientTimestamp(sessionId)
                Gossip(event = Event.SERVER_PONG, status = HttpResponseStatus.OK.code())
            }
            Event.CLIENT_RESPOND -> {
                sessionCacheService.savePayload(gossip)
                sessionCacheService.updateClientTimestamp(sessionId)
                Gossip(requestId = gossip.requestId, event = Event.SERVER_REQUEST_ACK, status = HttpResponseStatus.OK.code())
            }
            else -> Gossip(status = HttpResponseStatus.BAD_REQUEST.code(), message = "event:${gossip.event} not supported")
        })
    }
}