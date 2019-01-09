package com.ank.websockethttptunnel.client.integration.http

import com.ank.websockethttptunnel.client.config.ClientConfig
import com.ank.websockethttptunnel.client.exception.BadServerRequestException
import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import com.ank.websockethttptunnel.common.model.Gossip
import com.ank.websockethttptunnel.common.model.Payload
import com.ank.websockethttptunnel.common.util.toFlux
import com.ank.websockethttptunnel.common.util.toMultiValueMap
import io.netty.channel.ChannelOption
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import java.net.URLDecoder
import java.time.Duration
import javax.inject.Inject

@Service
class ClientWebHttpService @Inject constructor(
    val clientConfig: ClientConfig,
    val clientRequestElasticScheduler: Scheduler
) {
    companion object {
        val log = LoggerFactory.getLogger(ClientWebHttpService::class.java)
        val bufferFactory = DefaultDataBufferFactory()
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
                .clientConnector(ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .build()
    }

    fun getResponseFromLocalServer(payload: Payload?): Flux<Payload> {
        return payload?.method?.let {
            webHttpClient.method(payload.method).uri {
                it.path(URLDecoder.decode(payload.url.orEmpty(), "UTF-8"))
                        .queryParams(payload.queryParams.toMultiValueMap())
                        .build()
            }.headers { headers ->
                payload.headers?.forEach { header ->
                    if (header.key.equals("host", true).not()) {
                        headers.set(header.key, header.value)
                    }
                }
            }.body(BodyInserters.fromObject(payload.body))
                    .exchange().toFlux()
                    .subscribeOn(clientRequestElasticScheduler)
                    .publishOn(clientRequestElasticScheduler)
                    .flatMap { response ->
                        response.body { inputMessage, _ ->
                            inputMessage.body.buffer(100).map {
                                val byteBuffer = bufferFactory.join(it).asByteBuffer()
                                val bytes = ByteArray(byteBuffer.capacity())
                                byteBuffer.get(bytes, 0, bytes.size)
                                byteBuffer.clear()
                                bytes
                            }.flatMap {
                                payload.copy(headers = response.headers().asHttpHeaders().toMultiValueMap(), body = it, status = response.rawStatusCode()).toFlux()
                            }
                        }
                    }.doOnError {
                        log.error("${ClientWebHttpService::getResponseFromLocalServer.name}, Error=${it.message}", it)
                    }.doOnNext {
                        log.info("${ClientWebHttpService::getResponseFromLocalServer.name}, Response=$it")
                    }.onErrorResume {
                        Payload(status = HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), body = "INTERNAL_SERVER_ERROR".toByteArray()).toMono()
                    }
        } ?: Flux.error(BadServerRequestException(Gossip(message = "Invalid HTTP Method")))
    }
}