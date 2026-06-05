package DotsBoxes.referee;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefereeTest {

    @Test
    void getInstanceShouldReturnSingleton() {
        assertSame(Referee.getInstance(), Referee.getInstance());
    }

    @Test
    void applyActionShouldUpdateScoresAndWinner() {
        Player p1 = new StaticPlayer(0);
        Player p2 = new StaticPlayer(1);
        Board board = new Board(2, 2);

        Referee referee = Referee.getInstance();
        referee.init(p1, p2, board);

        referee.applyAction(p1, new Action(Action.Type.HORIZONTAL, 0, 0));
        referee.applyAction(p2, new Action(Action.Type.HORIZONTAL, 1, 0));
        referee.applyAction(p1, new Action(Action.Type.VERTICAL, 0, 0));
        int gained = referee.applyAction(p2, new Action(Action.Type.VERTICAL, 0, 1));

        assertAll(
                () -> assertEquals(1, gained),
                () -> assertEquals(0, referee.getScore(p1)),
                () -> assertEquals(1, referee.getScore(p2)),
                () -> assertSame(p2, referee.getWinner())
        );
    }

    @Test
    void getWinnerShouldReturnNullOnTie() {
        Player p1 = new StaticPlayer(0);
        Player p2 = new StaticPlayer(1);
        Board board = new Board(2, 2);

        Referee referee = Referee.getInstance();
        referee.init(p1, p2, board);

        assertNull(referee.getWinner());
    }

    @Test
    void applyInvalidMovePenaltyShouldDecreasePlayerScore() {
        Player p1 = new StaticPlayer(0);
        Player p2 = new StaticPlayer(1);
        Board board = new Board(2, 2);

        Referee referee = Referee.getInstance();
        referee.init(p1, p2, board);

        referee.applyInvalidMovePenalty(p1);

        assertAll(
                () -> assertEquals(-1, referee.getScore(p1)),
                () -> assertEquals(0, referee.getScore(p2)),
                () -> assertSame(p2, referee.getWinner())
        );
    }

    private static final class StaticPlayer implements Player {
        private final int id;

        private StaticPlayer(int id) {
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
}
