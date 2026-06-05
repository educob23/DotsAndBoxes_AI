package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FirstValidActionStrategyTest {

    @Test
    void shouldReturnFirstHorizontalEdgeOnFreshBoard() {
        Board board = new Board(2, 2);
        FirstValidActionStrategy strategy = new FirstValidActionStrategy();

        Action action = strategy.selectAction(board, 0);

        assertEquals(new Action(Action.Type.HORIZONTAL, 0, 0), action);
    }

    @Test
    void shouldReturnFirstVerticalWhenHorizontalsAreFull() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);

        FirstValidActionStrategy strategy = new FirstValidActionStrategy();

        Action action = strategy.selectAction(board, 0);

        assertEquals(new Action(Action.Type.VERTICAL, 0, 0), action);
    }

    @Test
    void shouldReturnNullWhenBoardIsFinished() {
        Board board = new Board(2, 2);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        FirstValidActionStrategy strategy = new FirstValidActionStrategy();

        assertNull(strategy.selectAction(board, 0));
        assertEquals("First valid", strategy.getName());
    }
}
