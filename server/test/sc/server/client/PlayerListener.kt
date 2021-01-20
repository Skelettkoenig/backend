package sc.server.client

import sc.api.plugins.host.IPlayerListener
import sc.protocol.responses.ProtocolMessage

class PlayerListener : IPlayerListener {
    val requests: MutableList<ProtocolMessage> = mutableListOf()

    override fun onPlayerEvent(request: ProtocolMessage) {
        requests.add(request)
    }
}