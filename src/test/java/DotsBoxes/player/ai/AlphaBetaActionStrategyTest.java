package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlphaBetaActionStrategyTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private AlphaBetaActionStrategy ab(int depth) {
        return new AlphaBetaActionStrategy(depth, new AlphaBetaPruningObserver(), new NodeCounterObserver());
    }

    private AlphaBetaActionStrategy ab(int depth, AlphaBetaPruningObserver obs, NodeCounterObserver nc) {
        return new AlphaBetaActionStrategy(depth, obs, nc);
    }

    // -----------------------------------------------------------------------
    // Cas limites
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldReturnNullWhenNoActionAvailable() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        assertNull(ab(3).selectAction(board, 0));
    }

    @Test
    void selectActionShouldNotModifyOriginalBoard() {
        Board board = new Board(2, 3);
        int actionsBefore = board.getAvailableActions().size();

        ab(3).selectAction(board, 0);

        assertEquals(actionsBefore, board.getAvailableActions().size());
    }

    @Test
    void selectedActionShouldBeAmongAvailableActions() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        Action best = ab(2).selectAction(board, 0);

        assertTrue(board.getAvailableActions().contains(best));
    }

    @Test
    void selectActionShouldAlwaysReturnValidActionThroughoutGame() {
        Board board = new Board(2, 3);
        int currentPlayer = 0;

        while (!board.isFinished()) {
            Action action = ab(2).selectAction(board, currentPlayer);
            assertNotNull(action);
            assertTrue(board.isValid(action));
            int closed = board.apply(action, currentPlayer);
            if (closed == 0) currentPlayer = 1 - currentPlayer;
        }
    }

    // -----------------------------------------------------------------------
    // Choix optimaux sur positions simples
    // -----------------------------------------------------------------------

    @Test
    void selectActionShouldChooseImmediateBoxClosingMoveAndIncrementNodeCounter() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        NodeCounterObserver nodeCounter   = new NodeCounterObserver();
        Action best = ab(2, observer, nodeCounter).selectAction(board, 1);

        assertAll(
                () -> assertEquals(new Action(Action.Type.VERTICAL, 0, 1), best),
                () -> assertTrue(nodeCounter.getCount() > 0),
                () -> assertTrue(observer.getAlphaCutCount() >= 0),
                () -> assertTrue(observer.getBetaCutCount() >= 0)
        );
    }

    @Test
    void selectActionShouldChooseBoxClosingMoveForPlayer0() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), ab(2).selectAction(board, 0));
    }

    @Test
    void selectActionShouldPreferMoveThatClosesTwoBoxes() {
        // Plateau 2x3 : seul VERTICAL(0,1) ferme les deux cases
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 2), 1);

        Action best = ab(3).selectAction(board, 0);

        assertAll(
                () -> assertEquals(new Action(Action.Type.VERTICAL, 0, 1), best),
                () -> assertEquals(2, new Board(board).apply(best, 0))
        );
    }

    @Test
    void selectActionShouldAvoidGivingFreeBoxToOpponent() {
        // Une case avec 3 côtés tracés : ne pas la compléter si un coup plus sûr existe
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        Action best = ab(3).selectAction(board, 1);

        assertNotEquals(new Action(Action.Type.VERTICAL, 0, 1), best);
        assertNotEquals(new Action(Action.Type.HORIZONTAL, 0, 0), best);
    }

    @Test
    void selectActionShouldPreferGivingOneBoxRatherThanTwoToOpponent() {
        // Identique au test Minimax équivalent : valider le même raisonnement stratégique
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 0);
        board.apply(new Action(Action.Type.VERTICAL, 1, 1), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 2, 1), 0);

        Action best = ab(3).selectAction(board, 1);

        assertNotEquals(new Action(Action.Type.VERTICAL, 1, 2), best);
        assertNotEquals(new Action(Action.Type.HORIZONTAL, 1, 1), best);
        assertNotEquals(new Action(Action.Type.VERTICAL, 0, 2), best);
    }

    // -----------------------------------------------------------------------
    // AlphaBeta == Minimax à profondeur égale
    // -----------------------------------------------------------------------

    /**
     * L'élagage alpha-bêta est une optimisation pure : il doit retourner
     * exactement le même coup que Minimax sur toute position avec un unique
     * coup optimal.
     */
    @Test
    void alphaBetaShouldReturnSameActionAsMinimaxWhenOneBoxAvailable() {
        // 3 côtés posés sur la seule case du plateau 2×2 → coup unique optimal
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        int depth = 3;
        Action mmAction = new MinimaxActionStrategy(depth).selectAction(board, 0);
        Action abAction = ab(depth).selectAction(board, 0);

        assertEquals(mmAction, abAction);
    }

    @Test
    void alphaBetaShouldReturnSameActionAsMinimaxWhenTwoBoxesAvailable() {
        // Seul VERTICAL(0,1) ferme les deux cases → coup unique optimal
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 2), 1);

        int depth = 3;
        Action mmAction = new MinimaxActionStrategy(depth).selectAction(board, 0);
        Action abAction = ab(depth).selectAction(board, 0);

        assertEquals(mmAction, abAction);
    }

    @Test
    void alphaBetaShouldReturnSameActionAsMinimaxOnFreshSmallBoard() {
        // Plateau vierge 2×2 : les deux algorithmes doivent choisir le même coup
        Board board = new Board(2, 2);

        int depth = 3;
        Action mmAction = new MinimaxActionStrategy(depth).selectAction(board, 0);
        Action abAction = ab(depth).selectAction(board, 0);

        // Même valeur d'évaluation → même action choisie
        assertEquals(mmAction, abAction);
    }

    @Test
    void alphaBetaShouldReturnSameActionAsMinimaxForPlayer1() {
        // Joueur 1 (minimiseur) face à une capture évidente
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        int depth = 2;
        Action mmAction = new MinimaxActionStrategy(depth).selectAction(board, 1);
        Action abAction = ab(depth).selectAction(board, 1);

        assertEquals(mmAction, abAction);
    }

    // -----------------------------------------------------------------------
    // L'élagage réduit effectivement le nombre de nœuds visités
    // -----------------------------------------------------------------------

    @Test
    void alphaBetaShouldVisitFewerNodesThanFullTreeOnDeepSearch() {
        // Sur un plateau non trivial en profondeur 4, des coupes doivent se produire
        Board board = new Board(2, 3);

        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        ab(4, observer, new NodeCounterObserver()).selectAction(board, 0);

        int totalCuts = observer.getAlphaCutCount() + observer.getBetaCutCount();
        assertTrue(totalCuts > 0, "Alpha-Beta devrait effectuer au moins une coupe");
    }

    @Test
    void alphaBetaShouldProducePruningCutsOnNonTrivialSearch() {
        Board board = new Board(2, 3);

        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        ab(4, observer, new NodeCounterObserver()).selectAction(board, 0);

        int totalCuts = observer.getAlphaCutCount() + observer.getBetaCutCount();
        assertTrue(totalCuts > 0, "Alpha-Beta devrait effectuer au moins une coupe");
    }

    // -----------------------------------------------------------------------
    // Observateurs
    // -----------------------------------------------------------------------

    @Test
    void nodeCounterAndObserverShouldCountSameNodes() {
        Board board = new Board(2, 3);

        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        NodeCounterObserver nodeCounter   = new NodeCounterObserver();
        ab(3, observer, nodeCounter).selectAction(board, 0);

        assertEquals(observer.getNodeCount(), nodeCounter.getCount());
    }

    @Test
    void observersShouldResetBetweenSuccessiveCalls() {
        Board board = new Board(2, 3);

        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        NodeCounterObserver nodeCounter   = new NodeCounterObserver();
        AlphaBetaActionStrategy strategy  = ab(2, observer, nodeCounter);

        strategy.selectAction(board, 0);
        int firstCount = nodeCounter.getCount();

        strategy.selectAction(board, 0);
        int secondCount = nodeCounter.getCount();

        assertEquals(firstCount, secondCount);
    }

    @Test
    void deeperDepthShouldVisitMoreNodes() {
        Board board = new Board(2, 3);

        NodeCounterObserver counter1 = new NodeCounterObserver();
        ab(1, new AlphaBetaPruningObserver(), counter1).selectAction(board, 0);
        int nodesDepth1 = counter1.getCount();

        NodeCounterObserver counter3 = new NodeCounterObserver();
        ab(3, new AlphaBetaPruningObserver(), counter3).selectAction(board, 0);
        int nodesDepth3 = counter3.getCount();

        assertTrue(nodesDepth3 > nodesDepth1);
    }

    // -----------------------------------------------------------------------
    // Nom
    // -----------------------------------------------------------------------

    @Test
    void getNameShouldReturnAlphaBeta() {
        assertEquals("Alpha-Beta", ab(1).getName());
    }
}
