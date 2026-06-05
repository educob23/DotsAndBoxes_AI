package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpertActionStrategyTest {

    private ExpertActionStrategy expert(int depth) {
        return new ExpertActionStrategy(depth);
    }

    // -----------------------------------------------------------------------
    // Constructeur / nom
    // -----------------------------------------------------------------------

    @Test
    void constructorShouldRejectInvalidDepth() {
        assertThrows(IllegalArgumentException.class, () -> new ExpertActionStrategy(0));
        assertThrows(IllegalArgumentException.class, () -> new ExpertActionStrategy(-1));
    }

    @Test
    void getNameShouldReturnExpert() {
        assertEquals("Expert", expert(4).getName());
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

        assertNull(expert(4).selectAction(board, 0));
    }

    @Test
    void selectActionShouldReturnOnlyAvailableMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), expert(4).selectAction(board, 1));
    }

    @Test
    void selectActionShouldNotModifyOriginalBoard() {
        Board board = new Board(4, 4);
        int before = board.getAvailableActions().size();

        expert(6).selectAction(board, 0);

        assertEquals(before, board.getAvailableActions().size());
    }

    @Test
    void selectedActionShouldBeValidAndAmongAvailableActions() {
        Board board = new Board(4, 4);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);

        Action chosen = expert(4).selectAction(board, 0);

        assertNotNull(chosen);
        assertTrue(board.isValid(chosen));
        assertTrue(board.getAvailableActions().contains(chosen));
    }

    @Test
    void selectActionShouldWorkForPlayer1() {
        Board board = new Board(3, 3);

        Action chosen = expert(4).selectAction(board, 1);

        assertNotNull(chosen);
        assertTrue(board.isValid(chosen));
    }

    // -----------------------------------------------------------------------
    // Phase 1 — capture immédiate
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldCaptureImmediatelyWhenBoxHasThreeSides() {
        // Box(0,0) a 3 côtés → VERTICAL(0,1) la ferme
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), expert(4).selectAction(board, 0));
    }

    @Test
    void selectActionShouldCaptureImmediatelyForPlayer1() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), expert(4).selectAction(board, 1));
    }

    @Test
    void selectActionShouldPreferCapturingTwoBoxesOverOne() {
        // VERTICAL(0,1) ferme simultanément Box(0,0) et Box(0,1)
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 2), 1);

        Action chosen = expert(4).selectAction(board, 0);

        assertAll(
            () -> assertEquals(new Action(Action.Type.VERTICAL, 0, 1), chosen),
            () -> assertEquals(2, new Board(board).apply(chosen, 0))
        );
    }

    @Test
    void selectActionShouldPreferImmediateClosureWhenAvailable() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 0);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), expert(4).selectAction(board, 1));
    }

    // -----------------------------------------------------------------------
    // Phase 2 — coup sûr (théorie des chaînes)
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldAvoidGivingThirdSideWhenSafeMoveExists() {
        // Board 2×3 : Box(0,0) a 2 côtés (H(0,0) + V(0,0))
        // H(1,0) et V(0,1) lui donneraient le 3e côté → coups dangereux
        // H(0,1), H(1,1), V(0,2) sont des coups sûrs
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);

        Action chosen = expert(6).selectAction(board, 0);

        assertNotEquals(new Action(Action.Type.HORIZONTAL, 1, 0), chosen,
                "Ne doit pas donner le 3e côté à Box(0,0) par le bas");
        assertNotEquals(new Action(Action.Type.VERTICAL,   0, 1), chosen,
                "Ne doit pas donner le 3e côté à Box(0,0) par la droite");
    }

    @Test
    void selectActionShouldAvoidOpeningLongChainWhenShortOneExists() {
        // Board 3×4 : une chaîne isolée de 1 case et une de 2 cases
        // L'Expert doit ouvrir la chaîne de 1 (sacrifice minimal)
        // Setup : Box(0,0)+Box(0,1) forment une chaîne de 2 (via V(0,1) libre)
        //         Box(1,2) est une chaîne de 1 (isolée)
        //         Toutes les autres cases ont < 2 côtés → des coups sûrs existent encore,
        //         mais on vérifie que si une chaîne doit être ouverte, c'est la plus courte.
        Board board = new Board(3, 4);
        // Box(0,0) : 2 côtés → H(0,0) + V(0,0)
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 0), 1);
        // Box(0,1) : 2 côtés → H(0,1) + V(0,2) (V(0,1) reste libre = lien chaîne)
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.VERTICAL,   0, 2), 1);
        // Box(1,2) : 2 côtés → H(2,2) + V(1,3) (isolée : voisins ont 0 côtés)
        board.apply(new Action(Action.Type.HORIZONTAL, 2, 2), 0);
        board.apply(new Action(Action.Type.VERTICAL,   1, 3), 1);

        // Des coups sûrs existent → l'Expert joue sûr (ne touche pas aux chaînes)
        Action chosen = expert(6).selectAction(board, 0);

        // Vérifier que l'action choisie ne donne pas de 3e côté aux chaînes longues
        // (Box(0,0) et Box(0,1) : ne pas jouer H(1,0), V(0,1), H(1,1))
        assertNotEquals(new Action(Action.Type.HORIZONTAL, 1, 0), chosen);
        assertNotEquals(new Action(Action.Type.VERTICAL,   0, 1), chosen);
        assertNotEquals(new Action(Action.Type.HORIZONTAL, 1, 1), chosen);
    }

    // -----------------------------------------------------------------------
    // Partie complète — cohérence tout au long du jeu
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldCompleteFullGameOnSmallBoard() {
        Board board = new Board(3, 3); // mode Alpha-Beta
        ExpertActionStrategy e = expert(8);
        int currentPlayer = 0;

        while (!board.isFinished()) {
            Action action = e.selectAction(board, currentPlayer);
            assertNotNull(action, "Action ne doit pas être null en cours de partie");
            assertTrue(board.isValid(action), "Action doit être valide");
            int closed = board.apply(action, currentPlayer);
            if (closed == 0) currentPlayer = 1 - currentPlayer;
        }

        assertTrue(board.isFinished());
    }

    @Test
    void selectActionShouldCompleteFullGameOnMediumBoard() {
        Board board = new Board(5, 5); // mode Alpha-Beta, 40 segments
        ExpertActionStrategy e = expert(15);
        int currentPlayer = 0;

        while (!board.isFinished()) {
            Action action = e.selectAction(board, currentPlayer);
            assertNotNull(action);
            assertTrue(board.isValid(action));
            int closed = board.apply(action, currentPlayer);
            if (closed == 0) currentPlayer = 1 - currentPlayer;
        }

        assertTrue(board.isFinished());
    }

    @Test
    void selectActionShouldCompleteFullGameOnLargeBoard() {
        Board board = new Board(7, 7); // mode glouton, 84 segments
        ExpertActionStrategy e = expert(10);
        int currentPlayer = 0;

        while (!board.isFinished()) {
            Action action = e.selectAction(board, currentPlayer);
            assertNotNull(action);
            assertTrue(board.isValid(action));
            int closed = board.apply(action, currentPlayer);
            if (closed == 0) currentPlayer = 1 - currentPlayer;
        }

        assertTrue(board.isFinished());
    }

    // -----------------------------------------------------------------------
    // Contrainte de temps
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldRespectTimeLimitOnLargeBoard() {
        // Mode glouton : 84 segments
        Board board = new Board(7, 7);

        long start = System.currentTimeMillis();
        expert(10).selectAction(board, 0);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 1000,
                "Premier coup sur 7×7 doit rester sous 1 s (était " + elapsed + " ms)");
    }

    @Test
    void selectActionShouldRespectTimeLimitOnSmallBoard() {
        // Mode Alpha-Beta avec approfondissement itératif limité à 850 ms
        Board board = new Board(5, 5);

        long start = System.currentTimeMillis();
        expert(20).selectAction(board, 0);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 1000,
                "Premier coup sur 5×5 doit rester sous 1 s (était " + elapsed + " ms)");
    }
}
