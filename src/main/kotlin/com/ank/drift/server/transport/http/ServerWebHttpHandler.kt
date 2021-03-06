package com.ank.drift.server.transport.http

import com.ank.drift.common.model.Payload
import com.ank.drift.common.util.toMultiValueMap
import com.ank.drift.server.service.ServerWebSocketRequestDelegator
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler
import javax.inject.Inject

@Service
class ServerWebHttpHandler @Inject constructor(
    val serverWebSocketRequestDelegator: ServerWebSocketRequestDelegator,
    val requestElasticScheduler: Scheduler
) {
    companion object {
        val log = LoggerFactory.getLogger(ServerWebHttpHandler::class.java)
        val bufferFactory = DefaultDataBufferFactory()
    }

    fun handle(): RouterFunction<ServerResponse> {
        return router {
            path("/**").and {
                !it.path().contains("/websocket")
            }.invoke { request ->
                    request.bodyToMono(ByteArray::class.java).defaultIfEmpty("".toByteArray()).flatMap { body ->
                                log.info("${ServerWebHttpHandler::handle.name}, Received request for path=${request.path()}")
                                val payload = Payload(method = request.method(),
                                        url = request.path(),
                                        queryParams = request.queryParams().toMultiValueMap(),
                                        headers = request.headers().asHttpHeaders().toMultiValueMap(),
                                        body = body)
                        serverWebSocketRequestDelegator.getResponse(payload).flatMap { responsePayload ->
                                    ServerResponse
                                            .status(responsePayload.status ?: 200)
                                            .headers { headers ->
                                                responsePayload.headers?.forEach {
                                                    if (it.key.equals("host", true).not()) {
                                                        headers[it.key] = it.value
                                                        log.info("url:${payload.url}, key:${it.key}, value:${it.value}")
                                                    }
                                                }
                                            }.body { outputMessage, _ ->
                                                outputMessage.writeWith(
                                                        bufferFactory.wrap(responsePayload.body).toMono())
                                            }
                                }.toMono()
                    }.doOnError {
                        log.error("${ServerWebHttpHandler::handle.name}, Error=${it.message}", it)
                    }.subscribeOn(requestElasticScheduler)
                            .publishOn(requestElasticScheduler)
            }
        }.filter { request, next ->
            next.handle(request)
        }
    }
}