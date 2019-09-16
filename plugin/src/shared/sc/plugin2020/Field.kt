package sc.plugin2020

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit
import sc.api.plugins.IField
import sc.plugin2020.util.CubeCoordinates
import sc.shared.PlayerColor
import java.util.*

@XStreamAlias("field")
class Field(
        x: Int = 0,
        y: Int = 0,
        z: Int = -x - y,
        // NOTE that an empty collection in XML gets deserialized to null, not to an empty collection!
        // see http://x-stream.github.io/faq.html#Serialization_implicit_null
        @XStreamImplicit
        var pieces: Stack<Piece> = Stack(),
        @XStreamAsAttribute
        val isObstructed: Boolean = false
): CubeCoordinates(x, y, z), IField {

    val fieldState: FieldState
        get() {
            if(isObstructed)
                return FieldState.OBSTRUCTED
            return when(owner) {
                PlayerColor.RED -> FieldState.RED
                PlayerColor.BLUE -> FieldState.BLUE
                null -> FieldState.EMPTY
            }
        }

    val isEmpty: Boolean
        get() = safePieces.isEmpty() && !isObstructed
    
    val hasOwner: Boolean
        get() = !safePieces.isEmpty() && !isObstructed

    val coordinates: CubeCoordinates
        get() = CubeCoordinates(this.x, this.y, this.z)

    val owner: PlayerColor?
        get() = if(safePieces.isEmpty()) null else safePieces.peek().owner
    
    val safePieces: Stack<Piece>
        get() {
            if(pieces == null) {
                pieces = Stack()
            }
            return pieces
        }

    constructor(position: CubeCoordinates, obstructed: Boolean): this(position.x, position.y, position.z, isObstructed = obstructed)

    constructor(position: CubeCoordinates, vararg pieces: Piece): this(position.x, position.y, position.z, pieces.toCollection(Stack()))

    constructor (x: Int, y: Int, obstructed: Boolean): this(x, y, isObstructed = obstructed)

    constructor(x: Int, y: Int, vararg pieces: Piece): this(x, y, pieces = pieces.toCollection(Stack()) as Stack<Piece>)

    constructor(field: Field): this(field.x, field.y, field.z, field.pieces.toCollection(Stack()), field.isObstructed)
    
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass)
            return if(other is CubeCoordinates) super.equals(other) else false

        other as Field

        if(x != other.x) return false
        if(y != other.y) return false
        if(z != other.z) return false
        if(safePieces != other.safePieces) return false
        if(isObstructed != other.isObstructed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + safePieces.hashCode()
        result = 31 * result + isObstructed.hashCode()
        return result
    }

}
