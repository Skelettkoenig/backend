package sc.plugin_minimal;

import java.io.IOException;

public class GuiClient extends AbstractClient
{
	public GuiClient(String host, int port, EPlayerId id) throws IOException
	{
		super(host, port, id);
	}

	private IGUIObservation	obs;

	public void setObservation(IGUIObservation obs)
	{
		this.obs = obs;
	}

	public IGUIObservation getObservation()
	{
		return obs;
	}

	private void sendLastTurn(Player oldPlayer, int playerId)
	{
		Move move = oldPlayer.getLastMove();
		obs.newTurn(playerId, GameUtil.displayMoveAction(move));
	}

	@Override
	public void onNewState(String roomId, Object state)
	{
		super.onNewState(roomId, state);

		GameState gameState = (GameState) state;
		Game game = gameState.getGame();

		if (obs != null)
		{
			Player oldPlayer = game.getBoard().getOtherPlayer(
					game.getActivePlayer());

			int playerid = 0;
			if (oldPlayer.getColor() == FigureColor.RED)
			{
				playerid = 0;
			}
			else
			{
				playerid = 1;
			}
			;

			if (oldPlayer.getLastMove() != null)
			{
				sendLastTurn(oldPlayer, playerid);
			}

			if (!alreadyReady)
			{
				alreadyReady = true;
				obs.ready();
			}
		}
	}

	@Override
	public void onGameJoined(String roomId)
	{
		if (obs != null)
		{
			obs.ready();
		}
	}

	@Override
	public void onGameLeft(String roomId)
	{
		// nothing to do
	}
}
