package com.ank.websockethttptunnel.client.integration.http

import com.ank.websockethttptunnel.client.config.ClientConfig
import com.ank.websockethttptunnel.client.exception.BadServerRequestException
import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.common.util.toMultiValueMap
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import java.time.Duration
import javax.inject.Inject

@Service
class WebHttpService @Inject constructor(val clientConfig: ClientConfig){
    companion object {
        val log = LoggerFactory.getLogger(WebHttpService::class.java)
    }

    val webHttpClient: WebClient by lazy {
        val tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofSeconds(clientConfig.localServer?.connectTimeoutInSec ?: TEN_SECONDS).toMillis().toInt())
                .doOnConnected { connection ->
                    connection.addHandlerLast(ReadTimeoutHandler((clientConfig.localServer?.readTimeoutInSec ?: TEN_SECONDS).toInt()))
                            .addHandlerLast(WriteTimeoutHandler((clientConfig.localServer?.readTimeoutInSec ?: TEN_SECONDS).toInt()))
                }
        WebClient.builder()
                .baseUrl(clientConfig.localServer?.url ?: "localhost")
                .exchangeStrategies(ExchangeStrategies.withDefaults())
                .clientConnector(ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .build()
    }

    fun getResponseFromLocalServer(payload: Payload?): Mono<Payload> {
        return payload?.method?.let {
            webHttpClient.method(payload.method).uri {
                it.path(payload.url.orEmpty())
                        .queryParams(payload.queryParams.toMultiValueMap())
                        .build()
            }.headers { headers ->
                payload.headers?.forEach { header ->
                    headers.set(header.key, header.value)
                }
            }.body(BodyInserters.fromObject(payload.body.orEmpty()))
                    .exchange()
                    .flatMap {response ->
                        response.bodyToMono(String::class.java).defaultIfEmpty("").map {
                            Payload(headers = response.headers().asHttpHeaders(), body = it, status = response.rawStatusCode())
                        }
                    }.doOnError {
                        log.error("${WebHttpService::getResponseFromLocalServer.name}, Error=${it.message}", it)
                    }.doOnNext {
                        log.info("${WebHttpService::getResponseFromLocalServer.name}, Response=$it")
                    }
        } ?: Mono.error<Payload>(BadServerRequestException(Gossip(message = "Invalid HTTP Method")))
    }
}