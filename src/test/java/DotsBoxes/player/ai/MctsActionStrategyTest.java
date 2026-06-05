package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MctsActionStrategyTest {

    // -----------------------------------------------------------------------
    // Constructeur
    // -----------------------------------------------------------------------

    @Test
    void constructorShouldRejectNonPositiveIterations() {
        assertThrows(IllegalArgumentException.class, () -> new MctsActionStrategy(0));
    }

    @Test
    void constructorShouldRejectNegativeIterations() {
        assertThrows(IllegalArgumentException.class, () -> new MctsActionStrategy(-5));
    }

    @Test
    void constructorShouldRejectNonPositiveIterationsWithSeed() {
        assertThrows(IllegalArgumentException.class, () -> new MctsActionStrategy(0, 42L));
    }

    // -----------------------------------------------------------------------
    // Cas limites
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldReturnNullWhenBoardIsFinished() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        MctsActionStrategy strategy = new MctsActionStrategy(300, 42L);

        assertNull(strategy.selectAction(board, 0));
    }

    @Test
    void selectActionShouldChooseTheOnlyAvailableMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        MctsActionStrategy strategy = new MctsActionStrategy(300, 7L);
        Action action = strategy.selectAction(board, 1);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), action);
    }

    // -----------------------------------------------------------------------
    // Correction de base
    // -----------------------------------------------------------------------


    @Test
    void selectedActionShouldBeAmongAvailableActions() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        MctsActionStrategy strategy = new MctsActionStrategy(400, 9L);
        Action action = strategy.selectAction(board, 0);

        List<Action> available = board.getAvailableActions();
        assertTrue(available.contains(action));
    }

    @Test
    void selectActionShouldNotModifyOriginalBoard() { //ne modifie pas le tableau original
        Board board = new Board(3, 3);
        int actionsBefore = board.getAvailableActions().size();

        MctsActionStrategy strategy = new MctsActionStrategy(500, 2L);
        strategy.selectAction(board, 0);

        assertEquals(actionsBefore, board.getAvailableActions().size());
    }

    @Test
    void selectActionShouldAlwaysReturnValidActionThroughoutGame() {
        Board board = new Board(2, 3);
        MctsActionStrategy strategy = new MctsActionStrategy(100, 77L);
        int currentPlayer = 0;

        while (!board.isFinished()) {
            Action action = strategy.selectAction(board, currentPlayer);
            assertNotNull(action);
            assertTrue(board.isValid(action));
            int closed = board.apply(action, currentPlayer);
            if (closed == 0) currentPlayer = 1 - currentPlayer;
        }
    }

    // -----------------------------------------------------------------------
    // Reproductibilité
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldBeReproducibleWithSameSeed() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);

        MctsActionStrategy s1 = new MctsActionStrategy(500, 123L);
        MctsActionStrategy s2 = new MctsActionStrategy(500, 123L);

        Action a1 = s1.selectAction(board, 0);
        Action a2 = s2.selectAction(board, 0);

        assertEquals(a1, a2);
        assertTrue(board.isValid(a1));
    }

    // -----------------------------------------------------------------------
    // Qualité des décisions
    // -----------------------------------------------------------------------

    /**
     * Plateau 3x3 avec 3 côtés de la case (0,0) tracés.
     * MCTS doit capturer la case disponible plutôt que de jouer ailleurs,
     * car cela donne un point immédiat et un tour supplémentaire.
     *
     * Situation :
     *   hEdge(0,0) = top    de box(0,0)
     *   hEdge(1,0) = bottom de box(0,0)
     *   vEdge(0,0) = left   de box(0,0)
     *   → VERTICAL(0,1) ferme box(0,0)
     */
    @Test
    void selectActionShouldCaptureAvailableBox() {
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        MctsActionStrategy strategy = new MctsActionStrategy(2000, 42L);
        Action action = strategy.selectAction(board, 0);

        // l'action choisie doit fermer au moins une case
        int closed = new Board(board).apply(action, 0);
        assertTrue(closed >= 1, "MCTS devrait saisir la case disponible");
    }

    /**
     * Plateau 3x3 : un seul coup (VERTICAL(0,1)) ferme deux cases à la fois.
     * Les 5 autres coups disponibles ne ferment aucune case.
     * MCTS doit identifier et jouer ce coup.
     *
     * Situation (boxes (0,0) et (0,1) ont chacune 3 côtés tracés) :
     *   hEdge(0,0), hEdge(0,1) = tops
     *   hEdge(1,0), hEdge(1,1) = bottoms
     *   vEdge(0,0) = left de box(0,0)
     *   vEdge(0,2) = right de box(0,1)
     *   → VERTICAL(0,1) ferme les deux cases d'un coup
     */
    @Test
    void selectActionShouldPreferMoveThatClosesTwoBoxes() {
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 2), 1);

        MctsActionStrategy strategy = new MctsActionStrategy(2000, 42L);
        Action action = strategy.selectAction(board, 0);

        int closed = new Board(board).apply(action, 0);
        assertEquals(2, closed, "MCTS devrait jouer le coup qui ferme les deux cases");
    }

    // -----------------------------------------------------------------------
    // Nom
    // -----------------------------------------------------------------------

    @Test
    void getNameShouldReturnMcts() {
        assertEquals("MCTS", new MctsActionStrategy(100).getName());
    }
}
