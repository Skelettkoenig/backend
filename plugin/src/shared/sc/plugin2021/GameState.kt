package sc.plugin2021

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import org.slf4j.LoggerFactory
import sc.api.plugins.ITeam
import sc.api.plugins.TwoPlayerGameState
import sc.framework.plugins.Player
import sc.plugin2021.util.Constants
import sc.plugin2021.util.GameRuleLogic

/**
 * Der aktuelle Spielstand.
 *
 * Er hält alle Informationen zur momentanen Runde,
 * mit deren Hilfe der nächste Zug berechnet werden kann.
 */
@XStreamAlias(value = "state")
class GameState @JvmOverloads constructor(
        /** Das erste Team, @see [Team]. */
        override var first: Player = Player(Team.ONE),
        /** Das zweite Team, @see [Team]. */
        override var second: Player = Player(Team.TWO),
        /** Der Spielstein, der in der ersten Runde gesetzt werden muss. */
        @XStreamAsAttribute val startPiece: PieceShape = GameRuleLogic.getRandomPentomino(),
        /** Das aktuelle Spielfeld. */
        @XStreamAsAttribute override val board: Board = Board(),
        turn: Int = 0,
        /** Der zuletzt gespielte Zug. */
        override var lastMove: Move? = null,
        /** Speichert für jede Farbe, die alle Steine gelegt hat, ob das Monomino zuletzt gelegt wurde. */
        @XStreamAsAttribute
        val lastMoveMono: MutableMap<Color, Boolean> = mutableMapOf()
): TwoPlayerGameState<Player>(Team.ONE) {
    
    companion object {
        val logger = LoggerFactory.getLogger(GameState::class.java)
    }
    
    constructor(other: GameState): this(other.first, other.second, other.startPiece, other.board.clone(), other.turn, other.lastMove, HashMap(other.lastMoveMono))
    
    private val blueShapes: MutableSet<PieceShape> = PieceShape.values().toMutableSet()
    private val yellowShapes: MutableSet<PieceShape> = PieceShape.values().toMutableSet()
    private val redShapes: MutableSet<PieceShape> = PieceShape.values().toMutableSet()
    private val greenShapes: MutableSet<PieceShape> = PieceShape.values().toMutableSet()
    
    /** Gib eine Liste aller nicht gesetzter Steine der [Color] zurück. */
    fun undeployedPieceShapes(color: Color = currentColor): MutableSet<PieceShape> = when (color) {
        Color.BLUE -> blueShapes
        Color.YELLOW -> yellowShapes
        Color.RED -> redShapes
        Color.GREEN -> greenShapes
    }
    
    /** Die Anzahl an bereits getätigten Zügen. */
    @XStreamAsAttribute
    override var turn: Int = turn
        set(value) {
            if (value < 0) throw IndexOutOfBoundsException("Can't go back in value (request was $turn to $value)")
            field = value
        }
    
    /** Die Rundenzahl. */
    @XStreamAsAttribute
    override var round: Int = 1
        private set
        get() {
            field = 1 + turn / orderedColors.size
            return field
        }
    
    /** Das Team, das am Zug ist. */
    override val currentTeam
        get() = currentColor.team
    
    /** Der Spieler, der am Zug ist. */
    override val currentPlayer
        get() = getPlayer(currentTeam)
    
    /** Liste aller Farben in ihrer Zugreihenfolge. */
    val orderedColors: List<Color>
        get() = Color.values().toList()
    
    /** Die Farbe, die am Zug ist. */
    val currentColor: Color
        get() = orderedColors[turn % Constants.COLORS]
    
    /** Liste der Farben, die noch im Spiel sind. */
    private val validColors: MutableSet<Color> = Color.values().toMutableSet()
    
    /** Beendet das Spiel, indem alle Farben entfernt werden. */
    internal fun clearValidColors() = validColors.clear()
    
    /** @return true, wenn noch Farben im Spiel sind. */
    fun hasValidColors() = validColors.isNotEmpty()

    /** Prüfe, ob die gegebene Farbe noch im Spiel ist. */
    fun isValid(color: Color = currentColor) = validColors.contains(color)

    /**
     * Versuche, zum nächsten Zug überzugehen.
     * Schlägt fehl, wenn das Spiel bereits zu ende ist.
     * @return Ob es erfolgreich war.
     */
    fun tryAdvance(turns: Int = 1): Boolean = try {
        turn += turns
        true
    } catch (e: Exception) {
        false
    }

    fun addPlayer(player: Player) {
        when (player.color) {
            Team.ONE -> first = player
            Team.TWO -> second = player
        }
    }

    /** Berechne die Punkteanzahl für das gegebene Team. */
    override fun getPointsForPlayer(team: ITeam): Int =
            (team as Team).colors.map { getPointsForColor(it) }.sum()

    private fun getPointsForColor(color: Color): Int {
        val pieces = undeployedPieceShapes(color)
        val lastMono = lastMoveMono[color] ?: false
        return GameRuleLogic.getPointsFromUndeployed(pieces, lastMono)
    }

    /**
     * Entferne die Farbe, die momentan am Zug ist.
     * Die resultierende aktive Farbe wird dann die des letzten Zuges sein.
     *
     * Diese Funktion wird von der [GameRuleLogic] benötigt und sollte nie so aufgerufen werden.
     */
    internal fun removeColor(color: Color = currentColor) {
        logger.info("Removing $color from the game")
        validColors.remove(color)
        logger.debug("Remaining Colors: $validColors")
    }

    override fun toString(): String =
            if (validColors.isEmpty()) "GameState finished at $round/$turn"
            else "GameState $round/$turn -> $currentColor ${if (GameRuleLogic.isFirstMove(this)) "(Start Piece: $startPiece)" else ""}"
    
    override fun clone() = GameState(this)
    
    override fun equals(other: Any?): Boolean {
        return this === other ||
               (other is GameState
                && first == other.first
                && second == other.second
                && startPiece == other.startPiece
                && board == other.board
                && lastMove == other.lastMove
                && lastMoveMono == other.lastMoveMono
                && turn == other.turn)
    }
    
    override fun hashCode(): Int {
        var result = first.hashCode()
        result = 31 * result + second.hashCode()
        result = 31 * result + startPiece.hashCode()
        result = 31 * result + board.hashCode()
        result = 31 * result + (lastMove?.hashCode() ?: 0)
        result = 31 * result + lastMoveMono.hashCode()
        result = 31 * result + turn
        return result
    }
    
}
