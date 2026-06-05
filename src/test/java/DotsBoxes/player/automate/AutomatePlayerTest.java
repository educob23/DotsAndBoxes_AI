package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AutomatePlayerTest {

    @Test
    void getActionShouldDelegateToStrategyWithPlayerId() {
        Board board = new Board(2, 2);
        Action expected = new Action(Action.Type.HORIZONTAL, 0, 0);
        RecordingStrategy strategy = new RecordingStrategy(expected, "Stub");

        AutomatePlayer player = new AutomatePlayer(0, strategy);

        Action actual = player.getAction(board);

        assertAll(
                () -> assertSame(expected, actual),
                () -> assertEquals(0, player.getId()),
                () -> assertEquals("Stub", player.getStrategyName()),
                () -> assertSame(board, strategy.lastBoard),
                () -> assertEquals(0, strategy.lastPlayerId)
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
