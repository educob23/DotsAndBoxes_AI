package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomActionStrategyTest {

    @Test
    void shouldReturnNullWhenNoMovesAvailable() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        RandomActionStrategy strategy = new RandomActionStrategy();

        assertNull(strategy.selectAction(board, 0));
    }

    @Test
    void shouldAlwaysReturnAValidAvailableMove() {
        Board board = new Board(2, 2);
        RandomActionStrategy strategy = new RandomActionStrategy();

        for (int i = 0; i < 50; i++) {
            Action action = strategy.selectAction(board, 1);
            assertNotNull(action);
            assertTrue(board.isValid(action));
            assertTrue(board.getAvailableActions().contains(action));
        }

        assertEquals("Random", strategy.getName());
    }
}
