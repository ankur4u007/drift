package com.ank.websockethttptunnel.client.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class ClientCacheService {
    companion object {
        val log = LoggerFactory.getLogger(ClientCacheService::class.java)
        val pingPongCount : AtomicLong = AtomicLong()
    }

    fun incrementPing(): Long {
        return pingPongCount.incrementAndGet()
    }

    fun decrementPong(): Long {
        return pingPongCount.decrementAndGet()
    }
}