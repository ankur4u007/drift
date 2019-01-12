package com.ank.drift.client.service

import com.ank.drift.client.config.ClientConfig
import com.ank.drift.common.contants.TEN_SECONDS
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.Disposable
import javax.inject.Inject

@Service
class ClientCacheService @Inject constructor(clientConfig: ClientConfig) {
    companion object {
        val log = LoggerFactory.getLogger(ClientCacheService::class.java)
    }

    private val circularFifoQueue = CircularFifoQueue<Boolean>((clientConfig.remoteServer?.ping?.reconnectAfterMaxMisses ?: TEN_SECONDS).toInt())
    private val staleResponses = mutableMapOf<String, Disposable>()

    fun updateAndCheckPingStatus(): Boolean {
        return synchronized(circularFifoQueue) {
            circularFifoQueue.add(true)
            if (circularFifoQueue.count { it == true } >= circularFifoQueue.maxSize()) {
                circularFifoQueue.clear()
                true
            } else {
                false
            }
        }
    }

    fun markForPong() {
        synchronized(circularFifoQueue) {
            circularFifoQueue.add(false)
        }
    }

    fun saveResponses(requestId: String, responseDisposable: Disposable) {
        staleResponses.putIfAbsent(requestId, responseDisposable)
    }

    fun acknowledgeResponse(requestId: String) {
        staleResponses.remove(requestId)
    }

    fun evictStaleResponses() {
        staleResponses.map {
            it.value.dispose()
            it.key
        }.forEach {
            staleResponses.remove(it)
        }
    }
}