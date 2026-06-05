package DotsBoxes.player.human;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HumanPlayerTest {

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;

    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    void shouldReadValidHorizontalAction() {
        System.setIn(new ByteArrayInputStream("H\n0\n0\n".getBytes()));

        HumanPlayer player = new HumanPlayer(0);
        Action action = player.getAction(new Board(2, 2));

        assertEquals(new Action(Action.Type.HORIZONTAL, 0, 0), action);
    }

    @Test
    void shouldRetryAfterInvalidTypeThenReturnAction() {
        System.setIn(new ByteArrayInputStream("X\nV\n0\n0\n".getBytes()));

        HumanPlayer player = new HumanPlayer(1);
        Action action = player.getAction(new Board(2, 2));

        assertEquals(new Action(Action.Type.VERTICAL, 0, 0), action);
        assertEquals(1, player.getId());
    }
}
