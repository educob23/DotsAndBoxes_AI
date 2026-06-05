package DotsBoxes.ui;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.Player;
import DotsBoxes.player.ai.AIPlayer;
import DotsBoxes.player.ai.AlphaBetaActionStrategy;
import DotsBoxes.player.ai.MinimaxActionStrategy;
import DotsBoxes.player.ai.ExpertActionStrategy;
import DotsBoxes.player.automate.AutomatePlayer;
import DotsBoxes.player.automate.GloutonActionStrategy;
import DotsBoxes.player.automate.RandomActionStrategy;
import DotsBoxes.referee.Referee;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.List;

/**
 * Interface graphique Swing pour jouer à Dots and Boxes.
 * <p>
 * Modes disponibles par joueur :
 * Humain, Automate (Glouton), Automate (Random), IA (Minimax), IA (Alpha-Beta), IA (Expert).
 * </p>
 * <p>
 * Le délai entre coups automatiques est configurable au lancement.
 * </p>
 */
public class DotsBoxesSwingUI {
    private static final int DEFAULT_AUTO_TURN_DELAY_MS = 1000;

    private final Board board;
    private final Referee referee;
    private final PlayerSlot slot1;
    private final PlayerSlot slot2;
    private PlayerSlot currentSlot;

    private final JFrame frame;
    private final JLabel turnLabel;
    private final JLabel scoreLabel;
    private final BoardPanel boardPanel;
    private final Timer autoTurnTimer;

    private DotsBoxesSwingUI(
            int rows,
            int cols,
            PlayerMode mode1,
            PlayerMode mode2,
            int aiDepth,
            int expertDepth,
            int autoTurnDelayMs
    ) {
        this.board = new Board(rows, cols);
        this.slot1 = createPlayerSlot(0, mode1, aiDepth, expertDepth);
        this.slot2 = createPlayerSlot(1, mode2, aiDepth, expertDepth);
        this.currentSlot = slot1;

        this.referee = Referee.getInstance();
        this.referee.init(slot1.player(), slot2.player(), board);

        this.frame = new JFrame("Dots and Boxes");
        this.turnLabel = new JLabel();
        this.scoreLabel = new JLabel();
        this.boardPanel = new BoardPanel();
        this.autoTurnTimer = new Timer(autoTurnDelayMs, e -> executeAutomatedTurnTick());
        this.autoTurnTimer.setRepeats(true);
        this.autoTurnTimer.setInitialDelay(autoTurnDelayMs);

        initUi();
        refreshStatus();
    }

    private static PlayerSlot createPlayerSlot(
            int id,
            PlayerMode mode,
            int aiDepth,
            int expertDepth
    ) {
        Player player = switch (mode) {
            case HUMAN -> new LocalHumanPlayer(id);
            case AUTO_GLOUTON -> new AutomatePlayer(id, new GloutonActionStrategy());
            case AUTO_RANDOM -> new AutomatePlayer(id, new RandomActionStrategy());
            case AI_MINIMAX -> new AIPlayer(id, new MinimaxActionStrategy(aiDepth));
            case AI_ALPHABETA -> new AIPlayer(
                    id,
                    new AlphaBetaActionStrategy(
                            aiDepth,
                            new AlphaBetaPruningObserver(),
                            new NodeCounterObserver()
                    )
            );
            case AI_EXPERT -> new AIPlayer(id, new ExpertActionStrategy(expertDepth));
        };
        return new PlayerSlot(player, mode);
    }

    private void initUi() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));

        turnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        scoreLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        header.add(turnLabel);
        header.add(scoreLabel);

        JButton resetButton = new JButton("Nouvelle partie");
        resetButton.addActionListener(e -> {
            frame.dispose();
            SwingUtilities.invokeLater(DotsBoxesSwingUI::launchFromDialog);
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        footer.add(resetButton, BorderLayout.EAST);

        frame.add(header, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(footer, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void show() {
        frame.setVisible(true);
        SwingUtilities.invokeLater(this::scheduleAutomatedTurnsIfNeeded);
    }

    private boolean isCurrentPlayerHuman() {
        return currentSlot.mode() == PlayerMode.HUMAN;
    }

    private void refreshStatus() {
        turnLabel.setText("Tour du joueur " + currentSlot.player().getId() + " (" + currentSlot.mode().label + ")");
        scoreLabel.setText(
                "Score J0: " + referee.getScore(slot1.player())
                        + "    Score J1: " + referee.getScore(slot2.player())
        );
    }

    private void switchPlayer() {
        currentSlot = (currentSlot == slot1) ? slot2 : slot1;
    }

    private void handleHumanAction(Action action) {
        if (!isCurrentPlayerHuman() || board.isFinished()) {
            return;
        }
        if (action == null) {
            return;
        }
        if (!board.isValid(action)) {
            referee.applyInvalidMovePenalty(currentSlot.player());
            switchPlayer();
            refreshStatus();
            boardPanel.repaint();
            scheduleAutomatedTurnsIfNeeded();
            return;
        }

        playActionForCurrentPlayer(action);
        scheduleAutomatedTurnsIfNeeded();
    }

    private void scheduleAutomatedTurnsIfNeeded() {
        if (board.isFinished() || isCurrentPlayerHuman()) {
            autoTurnTimer.stop();
            return;
        }
        if (!autoTurnTimer.isRunning()) {
            autoTurnTimer.start();
        }
    }

    private void executeAutomatedTurnTick() {
        if (board.isFinished() || isCurrentPlayerHuman()) {
            autoTurnTimer.stop();
            return;
        }

        Action action = currentSlot.player().getAction(board);
        if (action == null || !board.isValid(action)) {
            referee.applyInvalidMovePenalty(currentSlot.player());
            switchPlayer();
            refreshStatus();
            boardPanel.repaint();
            scheduleAutomatedTurnsIfNeeded();
            return;
        }

        playActionForCurrentPlayer(action);
        scheduleAutomatedTurnsIfNeeded();
    }

    private void playActionForCurrentPlayer(Action action) {
        int gained = referee.applyAction(currentSlot.player(), action);
        if (gained == 0) {
            switchPlayer();
        }

        refreshStatus();
        boardPanel.repaint();

        if (board.isFinished()) {
            autoTurnTimer.stop();
            Player winner = referee.getWinner();
            String message = (winner == null)
                    ? "Match nul"
                    : "Le joueur " + winner.getId() + " gagne";
            JOptionPane.showMessageDialog(frame, message, "Partie terminée", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static int promptIntInRange(String label, int defaultValue, int min, int max) {
        while (true) {
            String value = JOptionPane.showInputDialog(null, label, String.valueOf(defaultValue));
            if (value == null) {
                return -1;
            }
            try {
                int n = Integer.parseInt(value.trim());
                if (n >= min && n <= max) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
                // Retry
            }
            JOptionPane.showMessageDialog(null, "Entrez une valeur entre " + min + " et " + max + ".");
        }
    }

    private static PlayerMode promptPlayerMode(int playerId) {
        Object selected = JOptionPane.showInputDialog(
                null,
                "Choisissez le mode du joueur " + playerId,
                "Configuration joueur",
                JOptionPane.QUESTION_MESSAGE,
                null,
                PlayerMode.labels(),
                PlayerMode.HUMAN.label
        );

        if (selected == null) {
            return null;
        }
        return PlayerMode.fromLabel(selected.toString());
    }

    private static void launchFromDialog() {
        int rows = promptIntInRange("Nombre de lignes de points (2-12)", 4, 2, 12);
        if (rows < 0) return;

        int cols = promptIntInRange("Nombre de colonnes de points (2-12)", 4, 2, 12);
        if (cols < 0) return;

        PlayerMode mode1 = promptPlayerMode(0);
        if (mode1 == null) return;

        PlayerMode mode2 = promptPlayerMode(1);
        if (mode2 == null) return;

        int aiDepth = 3;
        if (mode1.isAi() || mode2.isAi()) {
            aiDepth = promptIntInRange("Profondeur de recherche IA (1-6)", 3, 1, 6);
            if (aiDepth < 0) return;
        }

        int expertDepth = 6;
        if (mode1.isExpert() || mode2.isExpert()) {
            expertDepth = promptIntInRange("Profondeur IA Expert (2-10)", 6, 2, 10);
            if (expertDepth < 0) return;
        }

        int autoTurnDelayMs = promptIntInRange(
                "Délai entre coups IA/automate (ms, 0-5000)",
                DEFAULT_AUTO_TURN_DELAY_MS,
                0,
                5000
        );
        if (autoTurnDelayMs < 0) return;

        new DotsBoxesSwingUI(
                rows,
                cols,
                mode1,
                mode2,
                aiDepth,
                expertDepth,
                autoTurnDelayMs
        ).show();
    }

    /**
     * Lance l'interface graphique.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(DotsBoxesSwingUI::launchFromDialog);
    }

    private enum PlayerMode {
        HUMAN("Humain"),
        AUTO_GLOUTON("Automate (Glouton)"),
        AUTO_RANDOM("Automate (Random)"),
        AI_MINIMAX("IA (Minimax)"),
        AI_ALPHABETA("IA (Alpha-Beta)"),
        AI_EXPERT("IA (Expert)");

        private final String label;

        PlayerMode(String label) {
            this.label = label;
        }

        private boolean isAi() {
            return this == AI_MINIMAX || this == AI_ALPHABETA || this == AI_EXPERT;
        }

        private boolean isExpert() {
            return this == AI_EXPERT;
        }

        private static String[] labels() {
            PlayerMode[] modes = values();
            String[] labels = new String[modes.length];
            for (int i = 0; i < modes.length; i++) {
                labels[i] = modes[i].label;
            }
            return labels;
        }

        private static PlayerMode fromLabel(String label) {
            for (PlayerMode mode : values()) {
                if (mode.label.equals(label)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Mode de joueur inconnu: " + label);
        }
    }

    private record PlayerSlot(Player player, PlayerMode mode) {
    }

    private static final class LocalHumanPlayer implements Player {
        private final int id;

        private LocalHumanPlayer(int id) {
            this.id = id;
        }

        @Override
        public Action getAction(Board board) {
            return null;
        }

        @Override
        public int getId() {
            return id;
        }
    }

    private final class BoardPanel extends JPanel {
        private static final int MARGIN = 30;
        private static final int CELL = 70;
        private static final int CLICK_THRESHOLD = 14;

        private BoardPanel() {
            int width = MARGIN * 2 + (board.getCols() - 1) * CELL;
            int height = MARGIN * 2 + (board.getRows() - 1) * CELL;
            setPreferredSize(new Dimension(width, height));
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Action action = findNearestAction(e.getX(), e.getY());
                    handleHumanAction(action);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            paintBoxes(g2);
            paintEdges(g2);
            paintDots(g2);

            g2.dispose();
        }

        private void paintBoxes(Graphics2D g2) {
            for (int r = 0; r < board.getRows() - 1; r++) {
                for (int c = 0; c < board.getCols() - 1; c++) {
                    int owner = board.getBoxOwner(r, c);
                    if (owner == 0) {
                        g2.setColor(new Color(120, 170, 255));
                    } else if (owner == 1) {
                        g2.setColor(new Color(255, 150, 150));
                    } else {
                        continue;
                    }
                    int x = MARGIN + c * CELL + 6;
                    int y = MARGIN + r * CELL + 6;
                    g2.fillRect(x, y, CELL - 12, CELL - 12);
                }
            }
        }

        private void paintEdges(Graphics2D g2) {
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols() - 1; c++) {
                    if (board.isHEdgeSet(r, c)) {
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawLine(px(c), py(r), px(c + 1), py(r));
                    }
                }
            }

            for (int r = 0; r < board.getRows() - 1; r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.isVEdgeSet(r, c)) {
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawLine(px(c), py(r), px(c), py(r + 1));
                    }
                }
            }
        }

        private void paintDots(Graphics2D g2) {
            g2.setColor(Color.BLACK);
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    int radius = 5;
                    g2.fillOval(px(c) - radius, py(r) - radius, 2 * radius, 2 * radius);
                }
            }
        }

        private Action findNearestAction(int x, int y) {
            List<Action> actions = board.getAvailableActions();
            Action best = null;
            double bestDistance = Double.MAX_VALUE;

            for (Action action : actions) {
                double d = distanceToAction(action, x, y);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = action;
                }
            }

            return bestDistance <= CLICK_THRESHOLD ? best : null;
        }

        private double distanceToAction(Action action, int x, int y) {
            if (action.getType() == Action.Type.HORIZONTAL) {
                int x1 = px(action.getCol());
                int y1 = py(action.getRow());
                int x2 = px(action.getCol() + 1);
                int y2 = y1;
                return Line2D.ptSegDist(x1, y1, x2, y2, x, y);
            }

            int x1 = px(action.getCol());
            int y1 = py(action.getRow());
            int x2 = x1;
            int y2 = py(action.getRow() + 1);
            return Line2D.ptSegDist(x1, y1, x2, y2, x, y);
        }

        private int px(int col) {
            return MARGIN + col * CELL;
        }

        private int py(int row) {
            return MARGIN + row * CELL;
        }
    }
}
