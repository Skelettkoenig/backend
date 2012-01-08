package sc.plugin2013;

import java.util.LinkedList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import sc.framework.plugins.SimplePlayer;
import sc.plugin2013.PlayerColor;

/**Ein Spieler, identifiziert durch seine Spielerfarbe.<br/>
 * Beeinhaltet auch Informationen zum Punktekonto und zu den {@link Card Karten).
 *
 * 
 * @author felix
 *
 */
@XStreamAlias(value = "cartagena:player")
public class Player extends SimplePlayer implements Cloneable {

	// Farbe des Spieler
	@XStreamOmitField
	private PlayerColor color;

	// Punkte des Spieler
	@XStreamAsAttribute
	private int points;

	// Liste der Karten, die sich auf der Hand befinden
	@XStreamImplicit(itemFieldName = "card")
	private final List<Card> cards;

	/**
	 * XStream benötigt eventuell einen parameterlosen Konstruktor bei der
	 * Deserialisierung von Objekten aus XML-Nachrichten.
	 */
	public Player() {
		cards = null;
	}

	/**
	 * Erzeugt einen neuen Spieler
	 * 
	 * @param color
	 *            Die Farbe des Spielers
	 * 
	 */
	public Player(final PlayerColor color) {
		this.color = color;
		this.cards = new LinkedList<Card>();
		this.points = 0;
	}
	
	 /**
     * klont dieses Objekt
     * @return ein neues Objekt mit gleichen Eigenschaften
     * @throws CloneNotSupportedException 
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        Player clone = new Player(this.color);
        clone.points = this.points;
        if (cards != null)
            for (Card c : this.cards)
                clone.addCard((Card)c.clone());
        return clone;
    }
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Player) && ((Player) obj).color == this.color;
	}

	/**
	 * Liefert die Farbe des Spielers
	 * 
	 * @return Die Farbe des Spielers
	 */
	public PlayerColor getPlayerColor() {
		return this.color;
	}
	
	/** 
	 * Fügt dem Spieler eine Karte hinzu
	 * @param card
	 */
	public void addCard(Card card){
		this.cards.add(card);
	}
	
	/** 
	 * Enfernt eine Karte mit übergebenem Symbol vom Spielerstapel
	 * @param symbol
	 */
	public void removeCard(SymbolType symbol){
		Card cardToRemove = null;
		for (Card card : cards) {
			if (card.symbol == symbol) {
				cardToRemove = card;
				break;
			}
		}
		cards.remove(cardToRemove);
	}
	
	/** Prüft ob ein Spieler eine Karte mit gegebenem Symbol besitzt
	 * @param symbol
	 * @return true wenn er eine Karte mit übergebenem Symbol besitzt
	 */
	public boolean hasCard(SymbolType symbol){
		for(Card card: cards){
			if(card.symbol == symbol){
				return true;
			}
		}
		return false;
	}
	
	/** 
	 * Liefert eine Liste, der Karten des Spielers
	 * @return
	 */
	public List<Card> getCards(){
		return this.cards;
	}
	
	/** 
	 * Fügt dem Punktekonto des Spieler Punkte hinzu
	 * @param points
	 */
	public void addPoints(int points){
		this.points += points;
	}
	
	/** 
	 * Liefert die Punkte des Spielers
	 * @return
	 */
	public int getPoints(){
		return this.points;
	}

}
