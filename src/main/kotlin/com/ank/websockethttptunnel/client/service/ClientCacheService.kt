package com.ank.websockethttptunnel.client.service

import com.ank.websockethttptunnel.client.config.ClientConfig
import com.ank.websockethttptunnel.common.contants.TEN_SECONDS
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class ClientCacheService @Inject constructor(clientConfig: ClientConfig) {
    companion object {
        val log = LoggerFactory.getLogger(ClientCacheService::class.java)
    }

    val circularFifoQueue = CircularFifoQueue<Boolean>((clientConfig.remoteServer?.ping?.reconnectAfterMaxMisses ?: TEN_SECONDS).toInt())

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
}