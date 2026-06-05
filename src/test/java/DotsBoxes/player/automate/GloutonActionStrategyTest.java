package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GloutonActionStrategyTest {

    @Test
    void shouldReturnRandomAvailableMoveWhenNoImmediateClosure() {
        Board board = new Board(2, 2);
        GloutonActionStrategy strategy = new GloutonActionStrategy(42L);

        Action action = strategy.selectAction(board, 0);

        assertTrue(board.getAvailableActions().contains(action));
        assertEquals(0, new Board(board).apply(action, 0));
    }

    @Test
    void shouldBeDeterministicWithSameSeedWhenNoClosureExists() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 2), 1);

        GloutonActionStrategy s1 = new GloutonActionStrategy(123L);
        GloutonActionStrategy s2 = new GloutonActionStrategy(123L);

        Action a1 = s1.selectAction(board, 0);
        Action a2 = s2.selectAction(board, 0);

        assertEquals(a1, a2);
    }

    @Test
    void shouldCloseABoxImmediatelyWhenPossible() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        GloutonActionStrategy strategy = new GloutonActionStrategy(7L);

        Action action = strategy.selectAction(board, 1);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), action);
    }

    @Test
    void shouldPreferMoveThatClosesTwoBoxesWhenAvailable() {
        Board board = new Board(2, 3);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 2), 1);

        GloutonActionStrategy strategy = new GloutonActionStrategy(999L);
        Action action = strategy.selectAction(board, 0);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 1), action);
        assertEquals(2, new Board(board).apply(action, 0));
    }

    @Test
    void shouldAlwaysReturnAValidActionWhenBoardIsNotFinished() {
        Board board = new Board(3, 3);
        GloutonActionStrategy strategy = new GloutonActionStrategy(123L);

        for (int i = 0; i < 20; i++) {
            Action action = strategy.selectAction(board, 0);
            assertTrue(board.isValid(action));
        }
    }

    @Test
    void shouldReturnNullWhenBoardIsFinished() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        GloutonActionStrategy strategy = new GloutonActionStrategy(1L);

        assertNull(strategy.selectAction(board, 0));
        assertEquals("Glouton", strategy.getName());
    }
}
