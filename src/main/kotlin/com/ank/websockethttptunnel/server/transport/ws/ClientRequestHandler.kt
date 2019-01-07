package com.ank.websockethttptunnel.server.transport.ws

import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.client.exception.BaseException
import com.ank.websockethttptunnel.common.util.orEmpty
import com.ank.websockethttptunnel.server.service.AuthService
import com.ank.websockethttptunnel.server.service.ServerEventHandlerService
import com.ank.websockethttptunnel.server.service.SessionCacheService
import com.fasterxml.jackson.core.JsonParseException
import io.netty.handler.codec.http.HttpResponseStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.SerializationUtils
import org.springframework.util.StreamUtils
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import javax.inject.Inject

@Service
class ClientRequestHandler @Inject constructor (val authService: AuthService,
                                                val eventHandlerService: ServerEventHandlerService,
                                                val sessionCacheService: SessionCacheService) : WebSocketHandler {
    companion object {
        var log  = LoggerFactory.getLogger(ClientRequestHandler::class.java)
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        log.info("${ClientRequestHandler::handle.name}, sessionId=${session.id}")
        return authService.authenticate(session.handshakeInfo.uri.query, session.id).flatMap { gossip ->
            sessionCacheService.registerClient(session)
            val response = session.send(session.binaryMessage {
                it.wrap(SerializationUtils.serialize(gossip).orEmpty())
            }.toMono())
            val request = session.receive()
                    .flatMap {
                        val gossip = SerializationUtils.deserialize(StreamUtils.copyToByteArray(it.payload.asInputStream())) as Gossip
                        gossip.toMono().flatMap {
                            eventHandlerService.handle(it, session.id)
                        }.onErrorResume {
                            handleError(session, it)
                        }
                    }.flatMap { gossip ->
                        session.send(session.binaryMessage {
                            it.wrap(SerializationUtils.serialize(gossip).orEmpty())
                        }.toMono())
                    }
            Flux.merge(request, response)
        }.doOnError{
            handleError(session, it).subscribe()
        }.then()
    }

    fun handleError(session: WebSocketSession, throwable: Throwable) : Mono<Gossip> {
        log.error("${ClientRequestHandler::handleError.name}, sessionId=${session.id}", throwable)
        return when (throwable) {
            is JsonParseException -> {
                Mono.just(Gossip(message = "JSON_PARSE_EXCEPTION; Request is not valid JSON", status = HttpResponseStatus.BAD_REQUEST.code()))
            }
            is BaseException -> {
                session.send(session.binaryMessage {
                    it.wrap(SerializationUtils.serialize(throwable.gossip).orEmpty())
                }.toMono()).map { throwable.gossip }
            }
            else -> {
                val gossip = Gossip(message = throwable.message, status = HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                session.send(session.binaryMessage {
                    it.wrap(SerializationUtils.serialize(gossip).orEmpty())
                }.toMono()).map { gossip }
            }
        }
    }

}