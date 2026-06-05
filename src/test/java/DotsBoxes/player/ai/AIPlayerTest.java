package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AIPlayerTest {

    @Test
    void getActionShouldDelegateToStrategyWithPlayerId() {
        Board board = new Board(2, 2);
        Action expected = new Action(Action.Type.HORIZONTAL, 0, 0);
        RecordingStrategy strategy = new RecordingStrategy(expected, "S1");

        AIPlayer player = new AIPlayer(1, strategy);

        Action actual = player.getAction(board);

        assertAll(
                () -> assertSame(expected, actual),
                () -> assertSame(board, strategy.lastBoard),
                () -> assertEquals(1, strategy.lastPlayerId)
        );
    }

    @Test
    void setMoveStrategyShouldReplaceStrategy() {
        ActionStrategy first = new RecordingStrategy(new Action(Action.Type.HORIZONTAL, 0, 0), "First");
        ActionStrategy second = new RecordingStrategy(new Action(Action.Type.VERTICAL, 0, 0), "Second");

        AIPlayer player = new AIPlayer(0, first);
        assertEquals("First", player.getMoveStrategyName());

        player.setMoveStrategy(second);

        assertAll(
                () -> assertSame(second, player.getMoveStrategy()),
                () -> assertEquals("Second", player.getMoveStrategyName()),
                () -> assertEquals(0, player.getId())
        );
    }

    private static final class RecordingStrategy implements ActionStrategy {
        private final Action actionToReturn;
        private final String name;
        private Board lastBoard;
        private int lastPlayerId = -1;

        private RecordingStrategy(Action actionToReturn, String name) {
            this.actionToReturn = actionToReturn;
            this.name = name;
        }

        @Override
        public Action selectAction(Board board, int playerId) {
            this.lastBoard = board;
            this.lastPlayerId = playerId;
            return actionToReturn;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
