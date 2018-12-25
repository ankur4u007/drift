package com.ank.websockethttptunnel.server.transport.http

import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.server.service.WebHttpSocketDelegator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
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
                    request.bodyToMono(String::class.java).defaultIfEmpty("").flatMap { bodyAsText ->
                                log.info("${WebHttpHandler::handle.name}, Received request for path=${request.path()}")
                                val payload = Payload(method = request.method(),
                                        url = request.path(),
                                        queryParams = request.queryParams().toMutableMap(),
                                        headers = request.headers().asHttpHeaders().toMutableMap(),
                                        body = bodyAsText)
                                webHttpSocketDelegator.getResponse(payload).flatMap { responsePayload ->
                                    ServerResponse
                                            .status(responsePayload.status ?: 200)
                                            .headers { headers ->
                                                responsePayload.headers?.forEach {
                                                    headers.set(it.key, it.value)
                                                }
                                            }.body(BodyInserters.fromObject(responsePayload.body.orEmpty()))
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