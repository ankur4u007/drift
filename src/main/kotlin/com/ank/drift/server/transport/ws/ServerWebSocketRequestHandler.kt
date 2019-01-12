package com.ank.drift.server.transport.ws

import com.ank.drift.common.model.Gossip
import com.ank.drift.client.exception.BaseException
import com.ank.drift.common.model.Event
import com.ank.drift.common.util.sendAsyncBinaryData
import com.ank.drift.server.service.AuthService
import com.ank.drift.server.service.ServerEventHandlerService
import com.ank.drift.server.service.SessionCacheService
import com.fasterxml.jackson.core.JsonParseException
import io.netty.handler.codec.http.HttpResponseStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.SerializationUtils
import org.springframework.util.StreamUtils
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler
import javax.inject.Inject

@Service
class ServerWebSocketRequestHandler @Inject constructor (
    val authService: AuthService,
    val serverEventHandlerService: ServerEventHandlerService,
    val sessionCacheService: SessionCacheService,
    val requestElasticScheduler: Scheduler,
    val registrationElasticScheduler: Scheduler,
    val pingElasticScheduler: Scheduler
) : WebSocketHandler {
    companion object {
        var log = LoggerFactory.getLogger(ServerWebSocketRequestHandler::class.java)
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        log.info("${ServerWebSocketRequestHandler::handle.name}, sessionId=${session.id}")
        return authService.authenticate(session.handshakeInfo.uri.query, session.id).flatMap { clientRegistration ->
            sessionCacheService.registerClient(session)
            session.sendAsyncBinaryData(clientRegistration, registrationElasticScheduler, log, this::handle.name)
            session.receive()
                    .flatMap {
                        val gossip = SerializationUtils.deserialize(StreamUtils.copyToByteArray(it.payload.asInputStream())) as Gossip
                        serverEventHandlerService.handle(gossip, session.id)
                    }.flatMap { gossip ->
                        if (gossip.event == Event.SERVER_PONG) {
                            session.sendAsyncBinaryData(gossip, pingElasticScheduler, log, this::handle.name).toMono()
                        } else {
                            session.sendAsyncBinaryData(gossip, requestElasticScheduler, log, this::handle.name).toMono()
                        }
                    }.doOnError {
                        handleError(session, it)
                    }
        }.doOnError {
            handleError(session, it)
        }.then()
    }

    fun handleError(session: WebSocketSession, throwable: Throwable): Mono<Disposable> {
        log.error("${ServerWebSocketRequestHandler::handleError.name}, sessionId=${session.id}", throwable)
        return when (throwable) {
            is JsonParseException -> {
                Mono.just(Gossip(message = "JSON_PARSE_EXCEPTION; Request is not valid JSON", status = HttpResponseStatus.BAD_REQUEST.code()))
            }
            is BaseException -> {
                Mono.just(throwable.gossip)
            }
            else -> {
                Mono.just(Gossip(message = throwable.message, status = HttpResponseStatus.INTERNAL_SERVER_ERROR.code()))
            }
        }.flatMap {
            session.sendAsyncBinaryData(it, requestElasticScheduler, log, this::handleError.name).toMono()
        }
    }
}