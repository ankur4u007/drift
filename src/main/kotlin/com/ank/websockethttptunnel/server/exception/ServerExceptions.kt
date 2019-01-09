package com.ank.websockethttptunnel.server.exception

import com.ank.websockethttptunnel.common.model.Gossip
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.RuntimeException

open class BaseException(open val gossip: Gossip) : RuntimeException(gossip.message)
class ForbiddenException(override val gossip: Gossip) : BaseException(gossip)
class BadRequestException(override val gossip: Gossip) : BaseException(gossip)

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class ClientUnavailableException(val msg: String = "Client unavailable currently.Try again after sometime") : RuntimeException(msg)

class ClientTimeoutException(val msg: String = "Client timedout. Try again after sometime") : RuntimeException(msg)