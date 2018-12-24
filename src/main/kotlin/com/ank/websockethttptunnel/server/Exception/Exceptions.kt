package com.ank.websockethttptunnel.server.Exception

import com.ank.websockethttptunnel.common.model.Gossip
import java.lang.RuntimeException

class ForbiddenException(val gossip: Gossip) : RuntimeException ()