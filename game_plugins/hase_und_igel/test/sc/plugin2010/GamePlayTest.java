package sc.plugin2010;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.sun.xml.internal.bind.v2.runtime.reflect.Accessor.GetterOnlyReflection;

import sc.api.plugins.exceptions.RescueableClientException;
import sc.api.plugins.exceptions.TooManyPlayersException;
import sc.plugin2010.Board.FieldTyp;
import sc.plugin2010.Move.MoveTyp;
import sc.plugin2010.Player.Action;
import sc.plugin2010.Player.FigureColor;

public class GamePlayTest
{
	private Game	g;
	private Board	b;
	private Player	red;
	private Player	blue;

	@Before
	public void beforeTest() throws TooManyPlayersException
	{
		g = new Game();
		b = g.getBoard();
		red = (Player) g.onPlayerJoined();
		blue = (Player) g.onPlayerJoined();
	}

	/**
	 * In der ersten Runde stehen beide Spieler am Start
	 */
	@Test
	public void firstRound()
	{
		Assert.assertEquals(red.getColor(), FigureColor.RED);
		Assert.assertEquals(blue.getColor(), FigureColor.BLUE);

		Assert.assertEquals(0, red.getFieldNumber());
		Assert.assertEquals(0, blue.getFieldNumber());
	}

	/**
	 * Wenn beide Spieler am Start stehen ist nur ein Zug möglich
	 */
	@Test
	public void justStarted()
	{
		Move m1 = new Move(MoveTyp.FALL_BACK);
		Assert.assertEquals(false, b.isValid(m1, red));

		Move m2 = new Move(MoveTyp.TAKE_OR_DROP_CARROTS, 10);
		Assert.assertEquals(false, b.isValid(m2, red));

		Move m3 = new Move(MoveTyp.PLAY_CARD, Action.EAT_SALAD);
		Assert.assertEquals(false, b.isValid(m3, red));

		Move m4 = new Move(MoveTyp.MOVE, b
				.getNextFieldByTyp(FieldTyp.CARROT, 0));
		Assert.assertEquals(true, b.isValid(m4, red));
	}

	/**
	 * Überprüft, dass Karotten nur auf einem Karottenfeld aufgenommen
	 * oder abgelegt werden dürfen
	 */
	@Test
	public void takeOrDropCarrots()
	{
		red.setFieldNumber(0);
		Move m = new Move(MoveTyp.TAKE_OR_DROP_CARROTS, 10);
		Assert.assertEquals(false, b.isValid(m, red));
		
		int rabbitAt = b.getNextFieldByTyp(FieldTyp.RABBIT, 0);
		red.setFieldNumber(rabbitAt);
		Assert.assertEquals(false, b.isValid(m, red));
		
		int saladAt = b.getNextFieldByTyp(FieldTyp.SALAD, 0);
		red.setFieldNumber(saladAt);
		Assert.assertEquals(false, b.isValid(m, red));
		
		int pos1 = b.getNextFieldByTyp(FieldTyp.POSITION_1, 0);
		red.setFieldNumber(pos1);
		Assert.assertEquals(false, b.isValid(m, red));
		
		int pos2 = b.getNextFieldByTyp(FieldTyp.POSITION_2, 0);
		red.setFieldNumber(pos2);
		Assert.assertEquals(false, b.isValid(m, red));
	}
	
	/**
	 * Überprüft, dass der Rundenzähler korrekt gesetzt wird. 
	 * @throws RescueableClientException 
	 */
	@Test
	public void turnCounting() throws RescueableClientException
	{
		g.start();
		
		red.setCarrotsAvailable(100);
		Assert.assertEquals(0, g.getTurn());
		
		int firstCarrot = b.getNextFieldByTyp(FieldTyp.CARROT, red.getFieldNumber());
		Move r1 = new Move(MoveTyp.MOVE, firstCarrot);
		g.onAction(red, r1);
		
		Assert.assertEquals(0, g.getTurn());
		
		int nextCarrot = b.getNextFieldByTyp(FieldTyp.CARROT, red.getFieldNumber());
		Move b1 = new Move(MoveTyp.MOVE, nextCarrot);
		Assert.assertEquals(blue, g.getActivePlayer());
		g.onAction(blue, b1);
		
		Assert.assertEquals(1, g.getTurn());
		
		int rabbitAt = b.getNextFieldByTyp(FieldTyp.RABBIT, red.getFieldNumber());
		Move r2 = new Move(MoveTyp.MOVE, rabbitAt-red.getFieldNumber());
		Assert.assertEquals(red, g.getActivePlayer());
		g.onAction(red, r2);
		
		Move r3 = new Move(MoveTyp.PLAY_CARD, Action.TAKE_OR_DROP_CARROTS, 20);
		Assert.assertEquals(red, g.getActivePlayer());
		g.onAction(red, r3);
		
		Assert.assertEquals(1, g.getTurn());
		
		nextCarrot = b.getNextFieldByTyp(FieldTyp.CARROT, blue.getFieldNumber());
		Move b2 = new Move(MoveTyp.MOVE, nextCarrot);
		Assert.assertEquals(blue, g.getActivePlayer());
		g.onAction(blue, b2);
		
		Assert.assertEquals(2, g.getTurn());
	}
	
	/**
	 * Überprüft den Ablauf, das Ziel zu erreichen 
	 * @throws RescueableClientException 
	 */
	@Test
	public void enterGoalCycle() throws RescueableClientException
	{
		g.start();
		
		int lastCarrot = b.getPreviousFieldByTyp(FieldTyp.CARROT, 64);
		int preLastCarrot = b.getPreviousFieldByTyp(FieldTyp.CARROT, lastCarrot);
		red.setFieldNumber(lastCarrot);
		blue.setFieldNumber(preLastCarrot);
		
		red.setCarrotsAvailable(GameUtil.calculateCarrots(64-lastCarrot));
		blue.setCarrotsAvailable(GameUtil.calculateCarrots(64-preLastCarrot)+1);
		red.setSaladsToEat(0);
		blue.setSaladsToEat(0);
		
		Move r1 = new Move(MoveTyp.MOVE, 64-red.getFieldNumber());
		Move b1 = new Move(MoveTyp.MOVE, 64-blue.getFieldNumber());
		
		g.onAction(red, r1);
		Assert.assertTrue(red.inGoal());
		
		g.onAction(blue, b1);
		Assert.assertTrue(blue.inGoal());
		
		Assert.assertTrue(b.isFirst(red));
	}
	
	/**
	 * Überprüft die Bedingungen, unter denen das Ziel betreten werden kann
	 */
	@Test
	public void enterGoal()
	{
		int carrotAt = b.getPreviousFieldByTyp(FieldTyp.CARROT, 64);
		red.setFieldNumber(carrotAt);
		int toGoal = 64 - red.getFieldNumber();
		Move m = new Move(MoveTyp.MOVE, toGoal);
		Assert.assertFalse(b.isValid(m, red));
		
		red.setCarrotsAvailable(10);
		Assert.assertFalse(b.isValid(m, red));
		
		red.setSaladsToEat(0);
		Assert.assertTrue(red.getSaladsToEat() == 0);
		Assert.assertTrue(red.getCarrotsAvailable() <= 10);
		Assert.assertTrue(b.isValid(m, red));
	}
	
	/**
	 * Überprüft, dass Salate nur auf Salatfeldern gefressen werden dürfen
	 */
	@Test
	public void eatSalad()
	{
		int saladAt = b.getNextFieldByTyp(FieldTyp.SALAD, 0);
		red.setFieldNumber(saladAt);
		
		Move m = new Move(MoveTyp.EAT);
		Assert.assertTrue(b.isValid(m, red));
		
		red.setSaladsToEat(0);
		Assert.assertFalse(b.isValid(m, red));
	}
	
	/**
	 * Simuliert den Ablauf von Salat-Fressen
	 * @throws RescueableClientException 
	 */
	@Test
	public void eatSaladCycle() throws RescueableClientException
	{
		g.start();
		
		red.setCarrotsAvailable(100);
		int saladAt = b.getNextFieldByTyp(FieldTyp.SALAD,	0);
		Move r1 = new Move(MoveTyp.MOVE, saladAt);
		g.onAction(red, r1);
		
		Move b1 = new Move(MoveTyp.MOVE, b.getNextFieldByTyp(FieldTyp.CARROT, 0));
		g.onAction(blue, b1);
		
		int before = red.getSaladsToEat();
		Move r2 = new Move(MoveTyp.EAT);
		g.onAction(red, r2);
		Assert.assertEquals(before-1, red.getSaladsToEat());
	}
	
	/**
	 * Simuliert den Ablauf einen Hasenjoker auszuspielen
	 * @throws RescueableClientException 
	 */
	@Test
	public void playCardCycle() throws RescueableClientException
	{
		g.start();
		
		int rabbitAt = b.getNextFieldByTyp(FieldTyp.RABBIT, 0);
		Move r1 = new Move(MoveTyp.MOVE, rabbitAt);
		g.onAction(red, r1);
		
		Assert.assertTrue(red.getActions().contains(Action.TAKE_OR_DROP_CARROTS));
		Move r2 = new Move(MoveTyp.PLAY_CARD, Action.TAKE_OR_DROP_CARROTS, 20);
		Assert.assertEquals(red, g.getActivePlayer());
		g.onAction(red, r2);
		Assert.assertFalse(red.getActions().contains(Action.TAKE_OR_DROP_CARROTS));
	}
	
	/**
	 * Simuliert das Fressen von Karotten auf einem Karottenfeld
	 * 
	 * @throws RescueableClientException
	 */
	@Test
	public void takeCarrotsCycle() throws RescueableClientException
	{
		g.start();

		int carrotsAt = b.getNextFieldByTyp(FieldTyp.CARROT, 0);
		Move m1 = new Move(MoveTyp.MOVE, carrotsAt);
		g.onAction(red, m1);

		Move m2 = new Move(MoveTyp.MOVE, b.getNextFieldByTyp(FieldTyp.CARROT, red.getFieldNumber()));
		g.onAction(blue, m2);

		Move m3 = new Move(MoveTyp.TAKE_OR_DROP_CARROTS, 10);
		Assert.assertEquals(true, b.isValid(m3, red));
		int carrotsBefore = red.getCarrotsAvailable();
		
		g.onAction(red, m3);
		Assert.assertEquals(carrotsBefore + 10, red.getCarrotsAvailable());
	}

	/**
	 * Simuliert das Ablegen von Karotten auf einem Karottenfeld
	 * 
	 * @throws RescueableClientException
	 */
	@Test
	public void dropCarrotsCycle() throws RescueableClientException
	{
		g.start();

		int carrotsAt = b.getNextFieldByTyp(FieldTyp.CARROT, 0);
		Move m1 = new Move(MoveTyp.MOVE, carrotsAt);
		g.onAction(red, m1);

		Move m2 = new Move(MoveTyp.MOVE, b.getNextFieldByTyp(FieldTyp.CARROT, red.getFieldNumber()));
		g.onAction(blue, m2);

		Move m3 = new Move(MoveTyp.TAKE_OR_DROP_CARROTS, -10);
		Assert.assertEquals(true, b.isValid(m3, red));
		int carrotsBefore = red.getCarrotsAvailable();

		g.onAction(red, m3);
		Assert.assertEquals(carrotsBefore - 10, red.getCarrotsAvailable());
	}

	/**
	 * Auf einem Karottenfeld darf kein Hasenjoker gespielt werden
	 */
	@Test
	public void actioncardOnFieldTypCarrot()
	{
		int field = b.getNextFieldByTyp(FieldTyp.CARROT, 0);
		red.setFieldNumber(field);

		Move m1 = new Move(MoveTyp.PLAY_CARD, Action.EAT_SALAD);
		Assert.assertEquals(false, b.isValid(m1, red));

		Move m2 = new Move(MoveTyp.PLAY_CARD, Action.FALL_BACK);
		Assert.assertEquals(false, b.isValid(m2, red));

		Move m3 = new Move(MoveTyp.PLAY_CARD, Action.HURRY_AHEAD);
		Assert.assertEquals(false, b.isValid(m3, red));

		Move m4 = new Move(MoveTyp.PLAY_CARD, Action.TAKE_OR_DROP_CARROTS);
		Assert.assertEquals(false, b.isValid(m4, red));
	}

	/**
	 * Ein Spieler darf nicht direkt auf ein Igelfeld ziehen;
	 */
	@Test
	public void directMoveOntoHedgehog()
	{
		int hedgehog = b.getNextFieldByTyp(FieldTyp.HEDGEHOG, 0);

		// direkter zug
		Move m1 = new Move(MoveTyp.MOVE, hedgehog);
		Assert.assertEquals(false, b.isValid(m1, red));

		blue.setFieldNumber(hedgehog + 1);
		int rabbit = b.getNextFieldByTyp(FieldTyp.RABBIT, blue.getFieldNumber());
		red.setFieldNumber(rabbit);
		
		// mit fallback
		Move m2 = new Move(MoveTyp.PLAY_CARD, Action.FALL_BACK);
		Assert.assertEquals(false, b.isValid(m2, red));

		blue.setFieldNumber(hedgehog - 1);
		rabbit = b.getNextFieldByTyp(FieldTyp.RABBIT, 0);
		red.setFieldNumber(rabbit);

		// mit hurry ahead
		Move m3 = new Move(MoveTyp.PLAY_CARD, Action.HURRY_AHEAD);
		Assert.assertEquals(false, b.isValid(m3, red));
	}

	/**
	 * Ohne Hasenjoker darf man kein Hasenfeld betreten!
	 */
	@Test
	public void moveOntoRabbitWithoutCard()
	{
		int rabbit = b.getNextFieldByTyp(FieldTyp.RABBIT, 0);
		red.setActions(Arrays.asList(new Action[] {}));
		Move m = new Move(MoveTyp.MOVE, rabbit);
		Assert.assertEquals(false, b.isValid(m, red));
	}

	/**
	 * Indirekte Züge auf einen Igel sind nicht erlaubt
	 */
	@Test
	public void indirectHurryAheadOntoHedgehog()
	{
		int hedgehog = b.getNextFieldByTyp(FieldTyp.HEDGEHOG, 0);
		blue.setFieldNumber(hedgehog);

		int rabbit = b.getNextFieldByTyp(FieldTyp.RABBIT, 0);
		red.setActions(Arrays.asList(Action.HURRY_AHEAD));

		Move m = new Move(MoveTyp.MOVE, rabbit);
		Assert.assertEquals(false, b.isValid(m, red));
	}

	/**
	 * Indirekte Züge auf einen Hasen sind nur erlaubt, wenn man danach noch
	 * einen Hasenjoker anwenden kann.
	 */
	@Test
	public void indirectHurryAheadOntoRabbit()
	{
		int firstRabbit = b.getNextFieldByTyp(FieldTyp.RABBIT, 0);
		int secondRabbit = b
				.getNextFieldByTyp(FieldTyp.RABBIT, firstRabbit + 1);

		blue.setFieldNumber(secondRabbit - 1);
		red.setActions(Arrays.asList(Action.HURRY_AHEAD));

		Move m1 = new Move(MoveTyp.MOVE, firstRabbit);
		Assert.assertEquals(false, b.isValid(m1, red));

		red.setActions(Arrays.asList(new Action[] { Action.HURRY_AHEAD,
				Action.EAT_SALAD }));
		Assert.assertEquals(true, b.isValid(m1, red));
	}

	/**
	 * Ein Spieler darf sich auf ein Igelfeld zurückfallen lassen.
	 */
	@Test
	public void fallback()
	{
		int firstHedgehog = b.getNextFieldByTyp(FieldTyp.HEDGEHOG, 0);

		int carrotAfter = b.getNextFieldByTyp(FieldTyp.CARROT, firstHedgehog+1);
		red.setFieldNumber(carrotAfter);

		Move m = new Move(MoveTyp.FALL_BACK);
		Assert.assertTrue(b.isValid(m, red));
	}
	
	/**
	 * Simuliert den Verlauf einer Zurückfallen-Aktion
	 * @throws RescueableClientException 
	 */
	@Test
	public void fallbackCycle() throws RescueableClientException
	{
		g.start();
		
		int firstHedgehog = b.getNextFieldByTyp(FieldTyp.HEDGEHOG, 0);
		int carrotAfter = b.getNextFieldByTyp(FieldTyp.CARROT, firstHedgehog+1);
		
		Move r1 = new Move(MoveTyp.MOVE, carrotAfter);
		red.setCarrotsAvailable(200);
		g.onAction(red, r1);
		
		Move b1 = new Move(MoveTyp.MOVE, b.getNextFieldByTyp(FieldTyp.CARROT, 0));
		g.onAction(blue, b1);
		
		Move r2 = new Move(MoveTyp.FALL_BACK);
		int carrotsBefore = red.getCarrotsAvailable();
		int diff = red.getFieldNumber() - firstHedgehog;
		g.onAction(red, r2);
		
		Assert.assertEquals(carrotsBefore+diff*10, red.getCarrotsAvailable());
	}
}
