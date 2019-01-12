package com.ank.drift.server.config

import com.ank.drift.common.contants.SIXTY_SECONDS
import com.ank.drift.common.util.JsonConverter
import com.ank.drift.server.service.SessionCacheService
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import javax.inject.Inject

@Configuration
@EnableWebFlux
@ComponentScan(" com.ank.drift.server")
@ConditionalOnProperty(name = ["tunnel.server.enabled"], havingValue = "true")
class ServerWebConfiguration @Inject constructor(
    val serverConfig: ServerConfig,
    val sessionCacheService: SessionCacheService
) : WebFluxConfigurer {

    @Bean
    fun startStaleClientSessionEviction(evictionElasticScheduler: Scheduler): Disposable {
        return Flux.interval(Duration.ofSeconds(serverConfig.remoteClient?.evictDurationInSec ?: SIXTY_SECONDS))
                .map {
                    sessionCacheService.evictStaleClientSession()
                }.retry {
                    it is Exception
                }.subscribeOn(evictionElasticScheduler)
                .publishOn(evictionElasticScheduler)
                .subscribe()
    }

    @Bean
    fun startStalePayloadEviction(evictionElasticScheduler: Scheduler): Disposable {
        val intervalInSeconds: Long = 1
        return Flux.interval(Duration.ofSeconds(intervalInSeconds))
                .map {
                    sessionCacheService.evictStalePayloads(intervalInSeconds)
                }.retry {
                    it is Exception
                }.subscribeOn(evictionElasticScheduler)
                .publishOn(evictionElasticScheduler)
                .subscribe()
    }

    @Bean
    fun evictionElasticScheduler(): Scheduler {
        return Schedulers.newElastic("server-eviction-scheduler", 60)
    }

    @Bean
    fun registrationElasticScheduler(): Scheduler {
        return Schedulers.newElastic("Server-registration-scheduler", 60)
    }

    @Bean
    fun pingElasticScheduler(): Scheduler {
        return Schedulers.newElastic("server-ping-scheduler", 60)
    }

    @Bean
    fun requestElasticScheduler(): Scheduler {
        return Schedulers.newElastic("server-request-scheduler", 300)
    }

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(JsonConverter.objectMapper
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)))

        configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(JsonConverter.objectMapper
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)))
    }
}