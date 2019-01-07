package com.ank.websockethttptunnel.server.transport.http

import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.common.util.toMultiValueMap
import com.ank.websockethttptunnel.server.service.WebHttpSocketDelegator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import javax.inject.Inject

@Service
class WebHttpHandler @Inject constructor(val webHttpSocketDelegator: WebHttpSocketDelegator){
    companion object {
        val log = LoggerFactory.getLogger(WebHttpHandler::class.java)
    }

    fun handle() : RouterFunction<ServerResponse> {
        return router {
            path("/**").and {
               !it.path().contains("/websocket")
            }.invoke { request ->
                    request.bodyToMono(ByteArray::class.java).defaultIfEmpty("".toByteArray()).flatMap { body ->
                                log.info("${WebHttpHandler::handle.name}, Received request for path=${request.path()}")
                                val payload = Payload(method = request.method(),
                                        url = request.path(),
                                        queryParams = request.queryParams().toMultiValueMap(),
                                        headers = request.headers().asHttpHeaders().toMultiValueMap(),
                                        body = body)
                                webHttpSocketDelegator.getResponse(payload).flatMap { responsePayload ->
                                    ServerResponse
                                            .status(responsePayload.status ?: 200)
                                            .headers { headers ->
                                                responsePayload.headers?.forEach {
                                                    if (it.key.equals("host", true).not()) {
                                                        headers[it.key] = it.value
                                                        log.info("key:${it.key}, value:${it.value}")
                                                    }
                                                }
                                            }.body(BodyInserters.fromObject(responsePayload.body))
                                }
                    }.doOnError {
                        log.error("${WebHttpHandler::handle.name}, Error=${it.message}", it)
                    }
            }
        }.filter { request, next ->
            next.handle(request)
        }
    }
}