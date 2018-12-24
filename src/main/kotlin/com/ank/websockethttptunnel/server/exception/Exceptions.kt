package com.ank.websockethttptunnel.server.exception

import com.ank.websockethttptunnel.common.model.Gossip
import java.lang.RuntimeException

open class BaseException(open val gossip: Gossip) : RuntimeException(gossip.message)
class ForbiddenException(override val gossip: Gossip) : BaseException (gossip)
class BadRequestException(override val gossip: Gossip) : BaseException (gossip)