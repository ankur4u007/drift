package com.ank.websockethttptunnel.client.exception

import com.ank.websockethttptunnel.common.model.Gossip
import java.lang.RuntimeException

open class BaseException(open val gossip: Gossip) : RuntimeException(gossip.message)
class ClientNotRespondingException(override val gossip: Gossip) : BaseException(gossip)