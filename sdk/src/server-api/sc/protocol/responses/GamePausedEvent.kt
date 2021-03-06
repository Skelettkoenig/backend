package sc.protocol.responses

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import sc.api.plugins.ITeam

import sc.framework.plugins.Player

/**
 * Indicates that the game has been paused.
 *
 * @param nextPlayer the next Player to move after unpausing
 */
@XStreamAlias(value = "paused")
data class GamePausedEvent(
        @XStreamAsAttribute
        val nextPlayer: Player
): ProtocolMessage
