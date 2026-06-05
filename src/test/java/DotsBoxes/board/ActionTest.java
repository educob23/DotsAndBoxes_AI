package DotsBoxes.board;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionTest {

    @Test
    void constructorShouldThrowWhenTypeIsNull() {
        assertThrows(NullPointerException.class, () -> new Action(null, 0, 0));
    }

    @Test
    void gettersToStringEqualsHashCodeShouldWork() {
        Action a1 = new Action(Action.Type.HORIZONTAL, 1, 2);
        Action a2 = new Action(Action.Type.HORIZONTAL, 1, 2);
        Action different = new Action(Action.Type.VERTICAL, 1, 2);

        assertAll(
                () -> assertEquals(Action.Type.HORIZONTAL, a1.getType()),
                () -> assertEquals(1, a1.getRow()),
                () -> assertEquals(2, a1.getCol()),
                () -> assertEquals("HORIZONTAL(1,2)", a1.toString()),
                () -> assertEquals(a1, a2),
                () -> assertEquals(a1.hashCode(), a2.hashCode()),
                () -> assertNotEquals(a1, different),
                () -> assertNotEquals(a1, null)
        );
    }
}
