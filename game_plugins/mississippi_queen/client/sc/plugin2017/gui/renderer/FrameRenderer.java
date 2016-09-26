/**
 *
 */
package sc.plugin2017.gui.renderer;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import processing.core.PApplet;
import sc.plugin2017.Acceleration;
import sc.plugin2017.Action;
import sc.plugin2017.DebugHint;
import sc.plugin2017.EPlayerId;
import sc.plugin2017.Field;
import sc.plugin2017.FieldType;
import sc.plugin2017.GameState;
import sc.plugin2017.Move;
import sc.plugin2017.Player;
import sc.plugin2017.PlayerColor;
import sc.plugin2017.Turn;
import sc.plugin2017.WinCondition;
import sc.plugin2017.gui.renderer.primitives.Background;
import sc.plugin2017.gui.renderer.primitives.BoardFrame;
import sc.plugin2017.gui.renderer.primitives.GameEndedDialog;
import sc.plugin2017.gui.renderer.primitives.GuiBoard;
import sc.plugin2017.gui.renderer.primitives.GuiConstants;
import sc.plugin2017.gui.renderer.primitives.GuiTile;
import sc.plugin2017.gui.renderer.primitives.HexField;
import sc.plugin2017.gui.renderer.primitives.ProgressBar;
import sc.plugin2017.gui.renderer.primitives.SideBar;
import sc.plugin2017.util.InvalidMoveException;

/**
 * @author soeren
 */

public class FrameRenderer extends PApplet {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory
      .getLogger(FrameRenderer.class);

  private GameState currentGameState;
  private GameState backUp;
  private Move currentMove;

  private GuiBoard guiBoard;

  private Background background;

  private ProgressBar progressBar;
  private SideBar sideBar;
  private BoardFrame boardFrame;

  private boolean initialized = false;

  private LinkedHashMap<HexField, Action> stepPossible;
  private WinCondition winCondition;

  public FrameRenderer() {
    super();

    RenderConfiguration.loadSettings();

    background = new Background(this);
    guiBoard = new GuiBoard(this);
    progressBar = new ProgressBar(this);
    sideBar = new SideBar(this);
    boardFrame = new BoardFrame(this);
    stepPossible = new LinkedHashMap<HexField, Action>();
  }

  @Override
  public void setup() {
    super.setup();
    logger.debug("Dimension when creating board: (" + width + ","
        + height + ")");
    // choosing renderer from options - using P2D as default (currently it seems
    // that only the java renderer works).
    //
    // NOTE that setting the size needs to be the first action of the setup
    // method (as stated in the processing reference).
    if (RenderConfiguration.optionRenderer.equals("JAVA2D")) {
      logger.debug("Using Java2D as Renderer");
      size(width, height, JAVA2D);
    } else if (RenderConfiguration.optionRenderer.equals("P3D")) {
      logger.debug("Using P3D as Renderer");
      size(width, height, P3D);
    } else {
      logger.debug("Using P2D as Renderer");
      size(width, height, P2D);
    }
    smooth(RenderConfiguration.optionAntiAliasing); // Anti Aliasing

    GuiConstants.generateFonts(this);
    // same font is used everywhere
    textFont(GuiConstants.font);

    HexField.initImages(this);
    guiBoard.setup();
    // only draw when needed (application calls redraw() if needed). Letting the loop run results in 100% (or high) CPU activity
    noLoop();
    initialized = true;
  }

  @Override
  public void draw() {
    if (!initialized) {
      // do not try to draw before setup method was not called
      return;
    }
    background.draw();
    guiBoard.draw();
    progressBar.draw();
    sideBar.draw();
    boardFrame.draw();
    if (!gameActive()) {
      drawEndGameScreen(winCondition);
    }
  }

  public void endGame(WinCondition condition) {
    winCondition = condition;
  }

  private void drawEndGameScreen(WinCondition condition) {
    String winnerName = null;
    if (condition.winner == PlayerColor.RED) {
      winnerName = currentGameState.getRedPlayer().getDisplayName();
    } else if (condition.winner == PlayerColor.BLUE) {
      winnerName = currentGameState.getBluePlayer().getDisplayName();
    }
    GameEndedDialog.draw(this, condition, winnerName);
  }

  public void updateGameState(GameState gameState) {
    // FIXME: winCondition determines if the game end screen is drawn, when
    // going back in the replay/game, it has to be cleared. Setting it to null
    // here works, but there has to be a better way.
    winCondition = null;
    try {
      currentGameState = gameState.clone();
    } catch (CloneNotSupportedException e) {
      logger.error("Problem cloning gamestate", e);
    }
    currentMove = new Move();
    // needed for simulation of actions
    currentGameState.getRedPlayer().setMovement(currentGameState.getRedPlayer().getSpeed());
    currentGameState.getBluePlayer().setMovement(currentGameState.getBluePlayer().getSpeed());
    currentGameState.getCurrentPlayer().setFreeTurns(currentGameState.isFreeTurn() ? 2 : 1);
    currentGameState.getCurrentPlayer().setFreeAcc(1);
    // make backup of gameState
    try {
      backUp = currentGameState.clone();
    } catch (CloneNotSupportedException e) {
      logger.error("Clone of Backup failed", e);
    }

    if (gameState != null && gameState.getBoard() != null) {
      logger.debug("updating gui board gamestate");
      updateView(currentGameState);
    } else {
      logger.error("got gamestate without board");
    }

    redraw();
  }

  public void requestMove(int maxTurn, EPlayerId id) {
    logger.debug("request move with {} for player {}", maxTurn, id);
    updateView(currentGameState);
  }

  public Image getImage() {
    // TODO return an Image of the current board
    return null;
  }

  private void updateView(GameState gameState) {
    if (gameState != null && gameState.getBoard() != null) {
      gameState.getRedPlayer().setPoints(gameState.getPointsForPlayer(PlayerColor.RED));
      gameState.getBluePlayer().setPoints(gameState.getPointsForPlayer(PlayerColor.BLUE));
      boardFrame.update(gameState.getCurrentPlayerColor());
      sideBar.update(gameState.getCurrentPlayerColor(), gameState.getRedPlayer().getDisplayName(), gameState.getPointsForPlayer(PlayerColor.RED), gameState.getBluePlayer().getDisplayName(), gameState.getPointsForPlayer(PlayerColor.BLUE));
      guiBoard.update(gameState.getVisibleBoard(), gameState.getRedPlayer(),
          gameState.getBluePlayer(), gameState.getCurrentPlayerColor(), currentMove);
    } else {
      boardFrame.update(null);
      sideBar.update(null);
    }
    redraw();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    super.mouseMoved(e);
    guiBoard.mouseMoved(mouseX, mouseY);
    redraw();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if (currentPlayerIsHuman()) {

      boolean onSandbank = currentGameState.getCurrentPlayer().getField( currentGameState.getBoard()).getType() == FieldType.SANDBANK;
      int currentSpeed = currentGameState.getCurrentPlayer().getSpeed();
      switch (guiBoard.getClickedButton(mouseX, mouseY)) {
      case LEFT:
        if (!onSandbank) {
          currentMove.actions.add(new Turn(1));
        }
        break;
      case RIGHT:
        if (!onSandbank) {
          currentMove.actions.add(new Turn(-1));
        }
        break;
      case SPEED_UP:
        if (!onSandbank && currentSpeed < 6) {
          if (!currentMove.actions.isEmpty() && currentMove.actions.get(currentMove.actions.size() - 1).getClass() == Acceleration.class) {
            // if last action was acceleration, increase value
            Acceleration a = (Acceleration)currentMove.actions.get(currentMove.actions.size() - 1);
            if (a.acc == -1) {
              currentMove.actions.remove(a);
            } else {
              a.acc += 1;
            }
          } else {
            currentMove.actions.add(new Acceleration(1));
          }
        }
        break;
      case SPEED_DOWN:
        if (!onSandbank && currentSpeed > 1) {
          if (!currentMove.actions.isEmpty() && currentMove.actions.get(currentMove.actions.size() - 1).getClass() == Acceleration.class) {
            // if last action was acceleration, decrease value
            Acceleration a = (Acceleration)currentMove.actions.get(currentMove.actions.size() - 1);
            if (a.acc == 1) {
              currentMove.actions.remove(a);
            } else {
              a.acc -= 1;
            }
          } else {
            currentMove.actions.add(new Acceleration(-1));
          }
        }
        break;
      case SEND:
        sendMove();
        break;
      case CANCEL:
        try {
          currentGameState = backUp.clone();
          currentMove = new Move();
        } catch (CloneNotSupportedException ex) {
          logger.error("Clone of backup failed", ex);
        }
        updateGameState(currentGameState);
        break;
      case NONE:
        // if no button was clicked, check if a hex field was clicked
        HexField clicked = getFieldCoordinates(mouseX, mouseY);
        if (stepPossible.containsKey(clicked)) {
          currentMove.actions.add(stepPossible.get(clicked));
        }
        break;
      }

      // Gamestate needs always be reset, even when action list is empty because
      // the emptyness may be the result of a removed acceleration in which case
      // the velocity needs to be reset.
      try {
        currentGameState = backUp.clone();
      } catch (CloneNotSupportedException ex) {
        logger.error("Clone of backup failed", ex);
      }
      if (!currentMove.actions.isEmpty()) {
        try {
          // perform actions individually because it is a partial move and should not be checked for validity
          for (Action action : currentMove.actions) {
            action.perform(currentGameState, currentGameState.getCurrentPlayer());
          }
        } catch (InvalidMoveException invalMove) {
          logger.error("Failed to perform move of user, please report if this happens", invalMove);
        }
      }
      updateView(currentGameState);
    }
  }

  private void sendMove() {
    currentMove.setOrderInActions();
    if (!currentMoveValid(currentMove)) {
      if (JOptionPane.showConfirmDialog(null, "Der Zug ist ungültig. Durch senden des aktuellen Zuges werden Sie disqualifiziert. Zug wirklich senden?", "Senden", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
        // do not send move
        return;
      }
    }
    RenderFacade.getInstance().sendMove(currentMove);
  }

  // NOTE that this method assumes the given move was already performed on the currentGameState!
  private boolean currentMoveValid(Move move) {
    boolean allMovementPointsUsed = currentGameState.getCurrentPlayer().getMovement() == 0;
    boolean accelerationFirst = true;
    // test if any action after the first one is an acceleration action
    for (int i = 1; i < move.actions.size(); i++) {
      if (move.actions.get(i).getClass() == Acceleration.class) {
        accelerationFirst = false;
      }
    }
    return allMovementPointsUsed && accelerationFirst;
  }

  private HexField getFieldCoordinates(int x, int y) {
    HexField coordinates;

    for (GuiTile tile : guiBoard.getTiles()) {
      coordinates = tile.getFieldCoordinates(x,y);
      if(coordinates != null) {
        return coordinates;
      }
    }
    return null;
  }

  @Override
  public void keyPressed() {
    super.keyPressed();
    if (key == 'c' || key == 'C') {
      new RenderConfigurationDialog(FrameRenderer.this);
      redraw();
    }
  }

  public EPlayerId getId() {
    return RenderFacade.getInstance().getActivePlayer();
  }

  public void killAll() {
    noLoop();
    if(background != null) {
      background.kill();
    }
    if(guiBoard != null) {
      guiBoard.kill();
    }
    if(progressBar != null) {
      progressBar.kill();
    }
    if(sideBar != null) {
      sideBar.kill();
    }
    if(boardFrame != null) {
      boardFrame.kill();
    }
  }

  public boolean currentPlayerIsHuman() {
    return getId() != EPlayerId.OBSERVER;
  }

  public Player getCurrentPlayer() {
    if (currentGameState != null) {
      return currentGameState.getCurrentPlayer();
    } else {
      return null;
    }
  }

  public void setPossibleSteps(LinkedHashMap<HexField, Action> add) {
    stepPossible = add;
  }

  public Field getCurrentPlayerField() {
    if (currentGameState != null && currentGameState.getBoard() != null) {
      return currentGameState.getCurrentPlayer().getField(currentGameState.getBoard());
    } else {
      return null;
    }
  }

  public int getCurrentRound() {
    if (currentGameState != null) {
      return currentGameState.getRound();
    } else {
      return 0;
    }
  }

  public boolean gameActive() {
    return winCondition == null;
  }

  public List<DebugHint> getCurrentHints() {
    if (currentGameState != null && currentGameState.getLastMove() != null) {
      return currentGameState.getLastMove().getHints();
    } else {
      return Collections.emptyList();
    }
  }

  public List<Action> getCurrentActions() {
    if (currentMove != null && currentMove.actions != null) {
      return currentMove.actions;
    } else {
      return Collections.emptyList();
    }
  }

  public Player getCurrentOpponent() {
    if (currentGameState != null) {
      return currentGameState.getOtherPlayer();
    } else {
      return null;
    }
  }

  public boolean playerControlsEnabled() {
    return currentPlayerIsHuman();
  }
}
