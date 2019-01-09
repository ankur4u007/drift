package com.ank.websockethttptunnel.client.config

import com.ank.websockethttptunnel.client.service.ClientCacheService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import javax.inject.Inject

@Configuration
@ComponentScan("com.ank.websockethttptunnel.client")
@ConditionalOnProperty(name = ["tunnel.client.enabled"], havingValue = "true")
class ClientWebConfiguration @Inject constructor(val clientCacheService: ClientCacheService){

    @Bean
    fun clientRegistrationElasticScheduler(): Scheduler {
        return Schedulers.newElastic("client-registration-scheduler", 60)
    }

    @Bean
    fun clientPingElasticScheduler(): Scheduler {
        return Schedulers.newElastic("client-ping-scheduler", 60)
    }

    @Bean
    fun clientRequestElasticScheduler(): Scheduler {
        return Schedulers.newElastic("client-request-scheduler", 300)
    }

    @Bean
    fun clientEvictionElasticScheduler(): Scheduler {
        return Schedulers.newElastic("server-eviction-scheduler", 60)
    }

    @Bean
    fun startStaleResponseEviction(clientEvictionElasticScheduler: Scheduler): Disposable {
        val intervalInSeconds :Long = 10
        return Flux.interval(Duration.ofSeconds(intervalInSeconds))
                .map {
                    clientCacheService.evictStaleResponses()
                }.retry {
                    it is Exception
                }.subscribeOn(clientEvictionElasticScheduler)
                .publishOn(clientEvictionElasticScheduler)
                .subscribe()
    }
}