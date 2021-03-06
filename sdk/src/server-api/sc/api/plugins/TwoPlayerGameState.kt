package sc.api.plugins

import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamOmitField
import org.slf4j.LoggerFactory
import sc.framework.plugins.Player

abstract class TwoPlayerGameState<P : Player>(
        @XStreamAsAttribute val startTeam: ITeam
) : IGameState {
    
    @XStreamOmitField
    private val logger = LoggerFactory.getLogger(TwoPlayerGameState::class.java)
    
    abstract val first: P
    abstract val second: P
    abstract val board: IBoard

    /** List of all teams. */
    val players: List<P>
        get() = listOf(first, second)
    
    /** Die Namen der beiden Spieler. */
    val playerNames: Array<String>
        get() = arrayOf(first.displayName, second.displayName)
    
    /** The Team active in the current turn. */
    abstract val currentTeam: ITeam

    /** The Player whose team's turn it is. */
    open val currentPlayer: P
        get() = getPlayer(currentTeam)

    /** The player opposite to the currently active one. */
    val otherPlayer: P
        get() = getPlayer(otherTeam)

    /** The Team opposite to the currently active one. */
    val otherTeam: ITeam
        get() = currentTeam.opponent()

    /** Der Spieler, der das Spiel begonnen hat. */
    val startPlayer: P
        get() = getPlayer(startTeam)

    /** Letzter getaetigter Zug. */
    abstract val lastMove: IMove?

    fun getOpponent(player: P) =
            getPlayer(player.color.opponent())

    fun getPlayer(team: ITeam): P = players[team.index]
    
    /** Calculates the color of the current player from the [turn] and the [startTeam].
     * Based on the assumption that the current player switches every turn. */
    fun currentPlayerFromTurn(): ITeam =
            if(turn.rem(2) == 0) startTeam else startTeam.opponent()

    /** Gibt die angezeigte Punktzahl des Spielers zurueck. */
    abstract fun getPointsForPlayer(team: ITeam): Int

    override fun toString() =
            "GameState(turn=$turn,currentPlayer=${currentPlayer.color})"
    
    public abstract override fun clone(): IGameState
    
}