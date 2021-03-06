package com.ank.drift.client.exception

import com.ank.drift.common.model.Gossip
import java.lang.RuntimeException

open class BaseException(open val gossip: Gossip) : RuntimeException(gossip.message)

class ServerNotRespondingException(override val gossip: Gossip) : BaseException(gossip)

class BadServerRequestException(override val gossip: Gossip) : BaseException(gossip)