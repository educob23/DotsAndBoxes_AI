package DotsBoxes.observers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeCounterObserverTest {

    @Test
    void incrementAndResetShouldUpdateCounter() {
        NodeCounterObserver observer = new NodeCounterObserver();

        observer.increment();
        observer.increment();
        assertEquals(2, observer.getCount());

        observer.reset();
        assertEquals(0, observer.getCount());
    }
}
