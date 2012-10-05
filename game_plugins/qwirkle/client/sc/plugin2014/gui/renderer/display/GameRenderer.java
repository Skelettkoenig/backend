package sc.plugin2014.gui.renderer.display;

import static sc.plugin2014.gui.renderer.configuration.GUIConstants.*;
import static sc.plugin2014.gui.renderer.configuration.RenderConfiguration.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;
import java.util.List;
import javax.swing.JComponent;
import sc.plugin2014.GameState;
import sc.plugin2014.entities.*;
import sc.plugin2014.gui.renderer.RenderFacade;
import sc.plugin2014.gui.renderer.components.*;
import sc.plugin2014.gui.renderer.components.Button;
import sc.plugin2014.gui.renderer.configuration.GUIConstants;
import sc.plugin2014.gui.renderer.configuration.RenderConfiguration;
import sc.plugin2014.gui.renderer.listener.GameKeyAdapter;
import sc.plugin2014.gui.renderer.listener.LayMoveAdapter;
import sc.plugin2014.gui.renderer.util.RendererUtil;
import sc.plugin2014.moves.*;

public class GameRenderer extends JComponent {
    private static final long       serialVersionUID  = -7852533731353419771L;

    private PlayerColor             currentPlayer;
    private GameState               gameState;

    private BufferedImage           buffer;
    private boolean                 updateBuffer;
    private final Image             bgImage;
    private Image                   scaledBgImage;
    private final Image             progressIcon;

    private static final Object     LOCK              = new Object();

    public EMoveMode                moveMode          = EMoveMode.NONE;

    private final List<GUIStone>    redStones;
    private final List<GUIStone>    blueStones;
    public List<GUIStone>           sensetiveStones;
    public GUIStone                 selectedStone;
    public int                      dx, dy;
    public int                      ox, oy;

    private int                     turnToAnswer      = -1;
    private boolean                 gameEnded;

    private final MouseAdapter      layMouseAdapter   = new LayMoveAdapter(this);

    private final ComponentListener componentListener = new ComponentAdapter() {

                                                          @Override
                                                          public void componentResized(
                                                                  ComponentEvent e) {
                                                              resizeBoard();
                                                              repaint();
                                                          }

                                                      };

    public List<GUIStone>           toLayStones       = new ArrayList<GUIStone>();

    private final Button            actionButton;
    private final Button            takeBackButton;

    private final List<GUIStone>    animatedStones    = new ArrayList<GUIStone>();

    public GameRenderer() {
        updateBuffer = true;
        bgImage = RendererUtil.getImage("resource/game/bg.png");
        progressIcon = RendererUtil.getImage("resource/game/progress.png");
        redStones = new LinkedList<GUIStone>();
        blueStones = new LinkedList<GUIStone>();
        sensetiveStones = new LinkedList<GUIStone>();

        setDoubleBuffered(true);
        addComponentListener(componentListener);
        GameKeyAdapter gameKeyAdapter = new GameKeyAdapter(this);
        addKeyListener(gameKeyAdapter);
        setFocusable(true);
        requestFocusInWindow();

        RenderConfiguration.loadSettings();

        setLayout(null);

        actionButton = new Button("Zug abschließen");
        this.add(actionButton);

        actionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (actionButton.isEnabled()) {
                        sendMove();
                    }
                }
            }
        });

        actionButton.addKeyListener(gameKeyAdapter);

        takeBackButton = new Button("Steine zurücknehmen");
        this.add(takeBackButton);

        takeBackButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (takeBackButton.isEnabled()) {
                        if (moveMode == EMoveMode.LAY) {
                            for (GUIStone stone : toLayStones) {
                                stone.setHighlighted(false);
                                addStone(stone);
                            }
                            selectedStone = null;
                            toLayStones.clear();
                        }
                        else {
                            for (GUIStone guistone : sensetiveStones) {
                                guistone.setHighlighted(false);
                            }
                        }

                        moveMode = EMoveMode.NONE;

                        actionButton.setEnabled(false);
                        takeBackButton.setEnabled(false);

                        updateView();
                    }
                }
            }
        });

        takeBackButton.addKeyListener(gameKeyAdapter);

        resizeBoard();
        repaint();

        updateView();
    }

    public void updateGameState(GameState gameState) {

        if ((this.gameState != null)) {
            int turnDiff = gameState.getTurn() - this.gameState.getTurn();

            Move move = gameState.getLastMove();
            if (!myTurn()) {
                if ((move != null) && (turnDiff == 1)
                        && (move instanceof LayMove)) {
                    moveStonesToBoard((LayMove) move,
                            gameState.getOtherPlayerColor());
                }
            }
        }

        actionButton.setEnabled(false);
        takeBackButton.setEnabled(false);

        this.gameState = gameState;
        currentPlayer = gameState.getCurrentPlayer().getPlayerColor();
        updateBuffer = true;

        selectedStone = null;

        redStones.clear();
        for (Stone redStone : gameState.getRedPlayer().getStones()) {
            redStones.add(new GUIStone(redStone, gameState.getRedPlayer()
                    .getStones().indexOf(redStone)));
        }

        blueStones.clear();
        for (Stone blueStone : gameState.getBluePlayer().getStones()) {
            blueStones.add(new GUIStone(blueStone, gameState.getBluePlayer()
                    .getStones().indexOf(blueStone)));
        }

        gameEnded = gameState.gameEnded();

        if (currentPlayer == PlayerColor.RED) {
            sensetiveStones = redStones;
        }
        else {
            sensetiveStones = blueStones;
        }

        if (gameState.gameEnded()) {
            gameEnded = true;
            currentPlayer = gameState.winner();
        }

        repaint();

    }

    private synchronized void moveStonesToBoard(final LayMove move,
            final PlayerColor playerColor) {

        System.out.println("moving stones");

        final int FPS = 60;

        setEnabled(false);

        for (Entry<Stone, Field> stoneToField : move.getStoneToFieldMapping()
                .entrySet()) {

            Field targetField = stoneToField.getValue();
            GUIStone animatedStone = new GUIStone(stoneToField.getKey(), -1);

            animatedStones.add(animatedStone);

            if (playerColor == PlayerColor.RED) {
                int x = BORDER_SIZE + STUFF_GAP;
                int y = getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH
                        - STUFF_GAP - STONE_HEIGHT - 30;
                animatedStone.setX(x);
                animatedStone.setY(y);
            }
            else {
                int x = getWidth() - BORDER_SIZE - STUFF_GAP - STONE_WIDTH;
                int y = getHeight() - BORDER_SIZE - PROGRESS_BAR_HEIGTH
                        - STUFF_GAP - STONE_HEIGHT - 30;
                animatedStone.setX(x);
                animatedStone.setY(y);
            }

            final Point p = new Point(animatedStone.getX(),
                    animatedStone.getY());

            int boardOffsetX = GUIBoard.calculateOffsetX(
                    GUIConstants.BORDER_SIZE, getWidth()
                            - GUIConstants.BORDER_SIZE
                            - GUIConstants.SIDE_BAR_WIDTH);

            int boardOffsetY = GUIBoard.calculateOffsetY(
                    GUIConstants.BORDER_SIZE, getHeight() - STATUS_HEIGTH);

            final Point q = new Point(boardOffsetX
                    + (targetField.getPosX() * STONE_WIDTH), boardOffsetY
                    + (targetField.getPosY() * STONE_HEIGHT));

            if (OPTIONS[MOVEMENT]) {

                double pixelPerFrame = getWidth() / (1.5 * FPS);
                double dist = Math.sqrt(Math.pow(p.x - q.x, 2)
                        + Math.pow(p.y - q.y, 2));

                final int frames = (int) Math.ceil(dist / pixelPerFrame);
                final Point o = new Point(p.x, p.y);
                final Point dP = new Point(q.x - p.x, q.y - p.y);

                long start = System.currentTimeMillis();
                for (int frame = 0; frame < frames; frame++) {

                    p.x = o.x + (int) ((double) (frame * dP.x) / frames);
                    p.y = o.y + (int) ((double) (frame * dP.y) / frames);
                    animatedStone.moveTo(p.x, p.y);

                    updateView();

                    synchronized (LOCK) {
                        LOCK.notify();
                    }

                    try {
                        long duration = (start + ((frame + 1) * (1000 / FPS)))
                                - System.currentTimeMillis();
                        Thread.sleep(duration > 0 ? duration : 0);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                animatedStone.setHighlighted(true);
            }
        }

        animatedStones.clear();

        setEnabled(true);
    }

    public synchronized void updateView() {
        updateBuffer = true;
        repaint();
    }

    public synchronized void requestMove(final int turn) {
        turnToAnswer = turn;

        addMouseListener(layMouseAdapter);
        addMouseMotionListener(layMouseAdapter);
    }

    private boolean myTurn() {
        return turnToAnswer == gameState.getTurn();
    }

    private synchronized void sendMove() {
        removeMouseListener(layMouseAdapter);
        removeMouseMotionListener(layMouseAdapter);

        if (moveMode == EMoveMode.LAY) {
            LayMove layMove = new LayMove();
            for (GUIStone guistone : toLayStones) {
                layMove.layStoneOntoField(guistone.getStone(),
                        guistone.getField());
            }

            if (myTurn() && !gameEnded) {
                RenderFacade.getInstance().sendMove(layMove);
            }

            toLayStones.clear();
        }
        else if (moveMode == EMoveMode.EXCHANGE) {
            ArrayList<Stone> stonesToExchange = new ArrayList<Stone>();

            for (GUIStone guiStone : sensetiveStones) {
                if (guiStone.isHighlighted()) {
                    stonesToExchange.add(guiStone.getStone());
                }
            }

            if (myTurn() && !gameEnded) {
                ExchangeMove exchangeMove = new ExchangeMove(stonesToExchange);
                RenderFacade.getInstance().sendMove(exchangeMove);
            }
        }

        moveMode = EMoveMode.NONE;
    }

    private void resizeBoard() {

        int width = getWidth() - (2 * GUIConstants.BORDER_SIZE);
        int heigth = getHeight() - (2 * GUIConstants.BORDER_SIZE)
                - GUIConstants.PROGRESS_BAR_HEIGTH;

        if ((width > 0) && (heigth > 0)) {
            MediaTracker tracker = new MediaTracker(this);

            scaledBgImage = new BufferedImage(width, heigth,
                    BufferedImage.TYPE_3BYTE_BGR);
            scaledBgImage.getGraphics().drawImage(bgImage, 0, 0, width, heigth,
                    this);
            tracker.addImage(scaledBgImage, 0);
            try {
                tracker.waitForID(0);
            }
            catch (InterruptedException e) {
                // e.printStackTrace();
            }
        }

        System.gc();
        updateBuffer = true;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                OPTIONS[ANTIALIASING] ? RenderingHints.VALUE_ANTIALIAS_ON
                        : RenderingHints.VALUE_ANTIALIAS_OFF);

        if (updateBuffer) {
            fillBuffer();
        }

        g2.drawImage(buffer, 0, 0, getWidth(), getHeight(), this);

        if (gameState != null) {
            boolean dragging = (selectedStone != null)
                    && (moveMode != EMoveMode.EXCHANGE);

            Painter.paintDynamicComponents(g2, selectedStone, getWidth(),
                    getHeight(), gameState, redStones, blueStones, this,
                    dragging);

            for (GUIStone animatedStone : animatedStones) {
                animatedStone.draw(g2);
            }
        }

        if (gameEnded) {
            Painter.paintEndMessage(g2, gameState, getWidth(), getHeight());
        }

        actionButton.setBounds((getWidth() / 2) - 100, getHeight() - 110, 200,
                30);

        actionButton.paint(g);

        takeBackButton.setBounds((getWidth() / 2) - 100, getHeight() - 70, 200,
                30);
        takeBackButton.paint(g);
    }

    private void fillBuffer() {

        int imageType = OPTIONS[TRANSPARANCY] ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_BGR;
        buffer = new BufferedImage(getWidth(), getHeight(), imageType);
        Graphics2D g2 = (Graphics2D) buffer.getGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                OPTIONS[ANTIALIASING] ? RenderingHints.VALUE_ANTIALIAS_ON
                        : RenderingHints.VALUE_ANTIALIAS_OFF);

        boolean dragging = (selectedStone != null)
                && (moveMode != EMoveMode.EXCHANGE);

        Painter.paintStaticComponents(g2, getWidth(), getHeight(), this,
                scaledBgImage, gameState, toLayStones, this, dragging);
        if (gameState != null) {
            // printGameStatus(g2);
            Painter.paintSemiStaticComponents(g2, getWidth(), getHeight(),
                    gameState, progressIcon, this);
        }
        updateBuffer = false;
    }

    public Image getImage() {
        BufferedImage img;
        img = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_INT_RGB);
        paint(img.getGraphics());
        return img;
    }

    public void layStone(GUIStone stone) {
        if (stone != null) {
            Field belongingField = GUIBoard.getBelongingField(
                    gameState.getBoard(), GUIConstants.BORDER_SIZE,
                    GUIConstants.BORDER_SIZE, getWidth()
                            - GUIConstants.BORDER_SIZE
                            - GUIConstants.SIDE_BAR_WIDTH, getHeight()
                            - GUIConstants.STATUS_HEIGTH, stone);
            if ((belongingField != null) && belongingField.isFree()) {
                stone.setField(belongingField);
                removeStone(stone);
                moveMode = EMoveMode.LAY;
                stone.setHighlighted(true);
                toLayStones.add(stone);
                actionButton.setEnabled(true);
                takeBackButton.setEnabled(true);
            }
            else {
                stone.setHighlighted(false);
                addStone(stone);
                if (toLayStones.size() == 0) {
                    moveMode = EMoveMode.NONE;
                    actionButton.setEnabled(false);
                    takeBackButton.setEnabled(false);
                }
            }
        }
    }

    public void exchangeStone(GUIStone stone) {
        if (stone != null) {
            if (stone.isHighlighted()) {
                moveMode = EMoveMode.EXCHANGE;
                stone.setHighlighted(true);
                actionButton.setEnabled(true);
                takeBackButton.setEnabled(true);
            }
            else {
                stone.setHighlighted(false);
                if (toLayStones.size() == 0) {
                    moveMode = EMoveMode.NONE;
                    actionButton.setEnabled(false);
                    takeBackButton.setEnabled(false);
                }
            }
        }
    }

    public void removeStone(GUIStone stone) {
        if (currentPlayer == PlayerColor.RED) {
            redStones.remove(stone);
        }
        else {
            blueStones.remove(stone);
        }

        toLayStones.remove(stone);
    }

    public void addStone(GUIStone stone) {
        if (currentPlayer == PlayerColor.RED) {
            if (redStones.size() > stone.getOriginalPositionOnHand()) {
                redStones.add(stone.getOriginalPositionOnHand(), stone);
            }
            else {
                redStones.add(stone);
            }
        }
        else {
            if (blueStones.size() > stone.getOriginalPositionOnHand()) {
                blueStones.add(stone.getOriginalPositionOnHand(), stone);
            }
            else {
                blueStones.add(stone);
            }
        }
    }

    public void toogleExchangeStone(GUIStone stone) {
        stone.setHighlighted(!stone.isHighlighted());

        for (GUIStone guiStone : sensetiveStones) {
            if (guiStone.isHighlighted()) {
                takeBackButton.setEnabled(true);
                actionButton.setEnabled(true);
                return;
            }
        }

        takeBackButton.setEnabled(false);
        actionButton.setEnabled(false);
        moveMode = EMoveMode.NONE;
    }
}
