package DotsBoxes.board;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    void constructorShouldInitializeEmptyBoard() {
        Board board = new Board(2, 2);

        assertAll(
                () -> assertEquals(2, board.getRows()),
                () -> assertEquals(2, board.getCols()),
                () -> assertEquals(4, board.getAvailableActions().size()),
                () -> assertEquals(-1, board.getBoxOwner(0, 0)),
                () -> assertFalse(board.isFinished())
        );
    }

    @Test
    void isValidShouldCheckBoundsAndAlreadyPlayedEdges() {
        Board board = new Board(2, 2);
        Action valid = new Action(Action.Type.HORIZONTAL, 0, 0);

        assertTrue(board.isValid(valid));
        board.apply(valid, 0);

        assertAll(
                () -> assertFalse(board.isValid(valid)),
                () -> assertFalse(board.isValid(new Action(Action.Type.HORIZONTAL, 0, 1))),
                () -> assertFalse(board.isValid(new Action(Action.Type.VERTICAL, 1, 0)))
        );
    }

    @Test
    void applyShouldCloseSingleBoxAndUpdateScore() {
        Board board = new Board(2, 2);

        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);

        int closed = board.apply(new Action(Action.Type.VERTICAL, 0, 1), 1);

        assertAll(
                () -> assertEquals(1, closed),
                () -> assertEquals(1, board.getScore(1)),
                () -> assertEquals(0, board.getScore(0)),
                () -> assertEquals(1, board.getBoxOwner(0, 0)),
                () -> assertTrue(board.isFinished())
        );
    }

    @Test
    void applyShouldCloseTwoBoxesWithMiddleVerticalEdge() {
        Board board = new Board(2, 3);

        board.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 0), 1);
        board.apply(new Action(Action.Type.HORIZONTAL, 1, 1), 1);
        board.apply(new Action(Action.Type.VERTICAL, 0, 0), 0);
        board.apply(new Action(Action.Type.VERTICAL, 0, 2), 1);

        int closed = board.apply(new Action(Action.Type.VERTICAL, 0, 1), 0);

        assertAll(
                () -> assertEquals(2, closed),
                () -> assertEquals(2, board.getScore(0)),
                () -> assertEquals(0, board.getScore(1)),
                () -> assertEquals(0, board.getBoxOwner(0, 0)),
                () -> assertEquals(0, board.getBoxOwner(0, 1))
        );
    }

    @Test
    void applyShouldThrowOnInvalidAction() {
        Board board = new Board(2, 2);

        assertThrows(IllegalArgumentException.class,
                () -> board.apply(new Action(Action.Type.HORIZONTAL, 0, 1), 0));
    }

    @Test
    void copyConstructorShouldCreateDeepCopy() {
        Board original = new Board(2, 2);
        original.apply(new Action(Action.Type.HORIZONTAL, 0, 0), 0);

        Board copy = new Board(original);
        original.apply(new Action(Action.Type.VERTICAL, 0, 0), 1);

        assertAll(
                () -> assertTrue(copy.isHEdgeSet(0, 0)),
                () -> assertFalse(copy.isVEdgeSet(0, 0)),
                () -> assertEquals(3, copy.getAvailableActions().size())
        );
    }
}
