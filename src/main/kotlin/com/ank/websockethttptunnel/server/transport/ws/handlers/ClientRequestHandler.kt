package com.ank.websockethttptunnel.server.transport.ws.handlers

import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.util.parseToType
import com.ank.websockethttptunnel.common.util.writeValueAsString
import com.ank.websockethttptunnel.server.Exception.ForbiddenException
import com.ank.websockethttptunnel.server.service.AuthService
import com.ank.websockethttptunnel.server.service.EventHandlerService
import com.ank.websockethttptunnel.server.service.SessionCacheService
import com.fasterxml.jackson.core.JsonParseException
import io.netty.handler.codec.http.HttpResponseStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import javax.inject.Inject

@Service
class ClientRequestHandler @Inject constructor (val authService: AuthService,
                                                val eventHandlerService: EventHandlerService,
                                                val sessionCacheService: SessionCacheService) : WebSocketHandler {
    companion object {
        var log  = LoggerFactory.getLogger(ClientRequestHandler::class.java)
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        log.info("${ClientRequestHandler::handle.name}, sessionId=${session.id}")
        return authService.authenticate(session.handshakeInfo.uri.query, session.id).flatMap {
            sessionCacheService.registerClient(session)
            session.receive()
        }.flatMap {
            val payloadAsText = it.payloadAsText
            payloadAsText.parseToType(Gossip::class.java).flatMap {
                eventHandlerService.handle(it, session.id)
            }.onErrorResume {
                log.error("${ClientRequestHandler::handle.name}, sessionId=${session.id}", it)
                when(it) {
                    is JsonParseException ->  Mono.just(Gossip(message = "JSON_PARSE_EXCEPTION; Request:${payloadAsText} is not valid JSON", status = HttpResponseStatus.BAD_REQUEST.code()))
                    else -> Mono.just(Gossip(message = it.message, status = HttpResponseStatus.INTERNAL_SERVER_ERROR.code()))
                }
            }
        }.flatMap {
            session.send(session.textMessage(it.writeValueAsString()).toMono())
        }.onErrorResume {
            handleError(session, it)
         }.then()
    }

    fun handleError(session: WebSocketSession, throwable: Throwable): Mono<Void> {
        log.error("${ClientRequestHandler::handleError.name}, sessionId=${session.id}", throwable)
        return when (throwable) {
            is ForbiddenException -> {
                session.send(session.textMessage(throwable.gossip.writeValueAsString()).toMono())
            }
            else -> {
                session.send(session.textMessage(Gossip(message = throwable.message, status = HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).writeValueAsString())
                        .toMono())
            }
        }
    }

}