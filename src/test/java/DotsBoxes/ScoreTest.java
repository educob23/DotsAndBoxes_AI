package DotsBoxes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreTest {

    @Test
    void winDrawLoseShouldUpdateCountersAndPoints() {
        Score score = new Score();

        score.win();
        score.draw();
        score.lose();

        assertEquals(4, score.points);
        assertEquals(1, score.wins);
        assertEquals(1, score.draws);
        assertEquals(1, score.losses);
    }
}
