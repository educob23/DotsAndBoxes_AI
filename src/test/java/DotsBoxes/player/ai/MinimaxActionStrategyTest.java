package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinimaxActionStrategyTest {

    @Test
    void selectActionShouldReturnNullWhenNoActionAvailable() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(3);

        assertNull(strategy.selectAction(board, 0));
    }

    @Test
    void selectActionShouldChooseImmediateBoxClosingMove() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(2);

        Action best = strategy.selectAction(board, 1);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), best);
    }

    @Test
    void selectActionShouldPreferMoveThatClosesTwoBoxes() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 2), 1);

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(3);
        Action best = strategy.selectAction(board, 0);

        assertAll(
                () -> assertEquals(new Action(Action.Type.VERTICAL, 0, 1), best),
                () -> assertEquals(2, new Board(board).apply(best, 0))
        );
    }

    @Test
    void selectActionShouldAvoidGivingFreeBoxToOpponent() {
        // une case avec 3 côtés tracés : minimax ne doit pas la compléter s'il existe un autre coup qui ne donne rien à l'adversaire
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        // compléter HORIZONTAL(0,0) ou VERTICAL(0,1) permettrait à l'adversaire de faire une case

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(3);
        Action best = strategy.selectAction(board, 1);

        // minimax doit éviter de de faire ces actions
        assertNotEquals(new Action(Action.Type.VERTICAL, 0, 1), best);
        assertNotEquals(new Action(Action.Type.HORIZONTAL, 0, 0), best);
    }

    @Test
    void selectActionShouldPreferGivingOneBoxRatherThanTwoToOponent() {
        // une case avec 3 côtés tracés : minimax ne doit pas la compléter s'il existe un autre coup qui ne donne rien à l'adversaire
        Board board = new Board(3, 3);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 0);
        board.apply(new Action(Action.Type.VERTICAL, 1, 1), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0);

        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 2, 1), 0);

        // compléter HORIZONTAL(1,1), VERTICAL(1,2) ou VERTICAL(0,2) permettrait à l'adversaire de faire deux cases, alors qu'il y a des options qui ne laissent que une casse

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(3);
        Action best = strategy.selectAction(board, 1);

        // minimax doit éviter de de faire ces actions
        assertNotEquals(new Action(Action.Type.VERTICAL, 1, 2), best);
        assertNotEquals(new Action(Action.Type.HORIZONTAL, 1, 1), best);
        assertNotEquals(new Action(Action.Type.VERTICAL, 0, 2), best);
    }

/* 
//le test suivant évaluait si la profondeur maximale était respectée et donc si une petite profondeur
//fessait que l'IA donne un mouvement pas bon car il ne pouvait pas réfléchir aux coups trops profonds
//Or, avec l'heuristique l'IA devient plus performante même si la profondeur reste la même,
//donc elle ne tombe pas dans le piège.
    @Test
    void selectActionShouldRespectDepthParameter() {
        // on pose un board avec un piège : 
        //  depth=1 ne voit pas le piège.
        //  depth=3 voit et évite le piège.
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 0); 
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 1);

        MinimaxActionStrategy naif    = new MinimaxActionStrategy(1);
        MinimaxActionStrategy nonNaif = new MinimaxActionStrategy(3);

        Action naifAction = naif.selectAction(board, 0);
        Action nonNaifAction    = nonNaif.selectAction(board, 0);

        // depth=1 ne voit pas le piège et prend la première action libre
        assertEquals(new Action(Action.Type.HORIZONTAL, 0, 0), naifAction);
        // depth=3 voit le piège donc ne choisit pas l'action piégeuse 
        assertNotEquals(new Action(Action.Type.HORIZONTAL, 0, 0), nonNaifAction);
    }
*/

    @Test
    void selectActionShouldReturnValidAction() {
        Board board = new Board(3, 3);
        MinimaxActionStrategy strategy = new MinimaxActionStrategy(2);

        Action action = strategy.selectAction(board, 0);

        assertTrue(board.isValid(action));
    }

    @Test
    void selectActionShouldAlwaysReturnValidActionThroughoutGame() {
        Board board = new Board(3, 3);
        MinimaxActionStrategy strategy = new MinimaxActionStrategy(2);

        while (!board.isFinished()) {
            int currentPlayer = board.getScore(0) >= board.getScore(1) ? 0 : 1;
            Action action = strategy.selectAction(board, currentPlayer);
            assertNotNull(action);
            assertTrue(board.isValid(action));
            board.apply(action, currentPlayer);
        }
    }

    @Test
    void selectActionShouldNotModifyOriginalBoard() {
        Board board = new Board(2, 2);
        int actionsBefore = board.getAvailableActions().size();

        MinimaxActionStrategy strategy = new MinimaxActionStrategy(3);
        strategy.selectAction(board, 0);

        assertEquals(actionsBefore, board.getAvailableActions().size());
    }

    @Test
    void getNameShouldReturnMinimax() {
        assertEquals("Minimax", new MinimaxActionStrategy(1).getName());
    }
}
