package DotsBoxes.observers;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class AlphaBetaPruningObserverTest {

    @Test
    void incrementersAndResetShouldWork() {
        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();

        observer.incrementNodeCount();
        observer.incrementAlphaCut();
        observer.incrementBetaCut();

        assertAll(
                () -> assertEquals(1, observer.getNodeCount()),
                () -> assertEquals(1, observer.getAlphaCutCount()),
                () -> assertEquals(1, observer.getBetaCutCount())
        );

        observer.reset();

        assertAll(
                () -> assertEquals(0, observer.getNodeCount()),
                () -> assertEquals(0, observer.getAlphaCutCount()),
                () -> assertEquals(0, observer.getBetaCutCount())
        );
    }

    @Test
    void printStatsShouldContainCounters() {
        AlphaBetaPruningObserver observer = new AlphaBetaPruningObserver();
        observer.incrementNodeCount();
        observer.incrementAlphaCut();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        try {
            System.setOut(new PrintStream(out));
            observer.printStats();
        } finally {
            System.setOut(previous);
        }

        String printed = out.toString();
        assertAll(
                () -> assertTrue(printed.contains("Alpha cuts: 1")),
                () -> assertTrue(printed.contains("Beta cuts: 0")),
                () -> assertTrue(printed.contains("Nodes visited: 1"))
        );
    }
}
