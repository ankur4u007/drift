package com.ank.websockethttptunnel.server.transport.http

import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.common.util.writeValueAsString
import com.ank.websockethttptunnel.server.service.WebHttpSocketDelegator
import io.netty.handler.codec.http.HttpMethod
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
            log.info("${WebHttpHandler::handle.name}, 1 received request for ${this.writeValueAsString()}")
            path("/**").and {
               !it.path().contains("/websocket")
            }.invoke { request ->
                    log.info("${WebHttpHandler::handle.name}, 2 received request for ${this.writeValueAsString()}")
                    request.bodyToMono(String::class.java).defaultIfEmpty("").flatMap { bodyAsText ->
                                log.info("${WebHttpHandler::handle.name}, 3 received request for ${request.path()}")
                                val payload = Payload(method = HttpMethod.valueOf(request.methodName()),
                                        url = request.path(),
                                        queryParams = request.queryParams().toSingleValueMap(),
                                        headers = request.headers().asHttpHeaders().toMutableMap(),
                                        body = bodyAsText)
                                webHttpSocketDelegator.getResponse(payload).flatMap {
                                    ServerResponse.ok().body(BodyInserters.fromObject(it.body.orEmpty()))
                                }
                    }.doOnError {
                        log.error("${WebHttpHandler::handle.name}, Exception=${it.message}", it)
                    }
            }
        }.filter { request, next ->
            next.handle(request)
        }
    }
}