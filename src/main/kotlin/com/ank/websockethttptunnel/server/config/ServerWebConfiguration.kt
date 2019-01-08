package com.ank.websockethttptunnel.server.config

import com.ank.websockethttptunnel.common.contants.SIXTY_SECONDS
import com.ank.websockethttptunnel.common.util.JsonConverter
import com.ank.websockethttptunnel.server.service.SessionCacheService
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
@ComponentScan(" com.ank.websockethttptunnel.server")
@ConditionalOnProperty(name = ["tunnel.server.enabled"], havingValue = "true")
class ServerWebConfiguration @Inject constructor(
    val serverConfig: ServerConfig,
    val sessionCacheService: SessionCacheService
) : WebFluxConfigurer {

    @Bean
    fun startCacheEviction(registrationElasticScheduler: Scheduler): Disposable {
        return Flux.interval(Duration.ofSeconds(serverConfig.remoteClient?.evictDurationInSec ?: SIXTY_SECONDS))
                .map {
                    sessionCacheService.evictCache()
                }.retry {
                    it is Exception
                }.subscribeOn(registrationElasticScheduler)
                .subscribe()
    }

    @Bean
    fun registrationElasticScheduler(): Scheduler {
        return Schedulers.newElastic("registration-elastic-scheduler", 60)
    }

    @Bean
    fun pingElasticScheduler(): Scheduler {
        return Schedulers.newElastic("ping-elastic-scheduler", 60)
    }

    @Bean
    fun requestElasticScheduler(): Scheduler {
        return Schedulers.newElastic("request-elastic-scheduler", 300)
    }

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(JsonConverter.objectMapper
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)))

        configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(JsonConverter.objectMapper
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)))
    }
}