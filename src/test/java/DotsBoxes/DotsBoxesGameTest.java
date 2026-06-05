package DotsBoxes;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.Player;
import DotsBoxes.player.ai.AIPlayer;
import DotsBoxes.player.automate.AutomatePlayer;
import DotsBoxes.referee.Referee;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotsBoxesGameTest {

    @Test
    void playShouldReturnWinnerOnSimpleGame() {
        ScriptedPlayer p1 = new ScriptedPlayer(0,
                new Action(Action.Type.HORIZONTAL, 0, 0),
                new Action(Action.Type.VERTICAL, 0, 0));
        ScriptedPlayer p2 = new ScriptedPlayer(1,
                new Action(Action.Type.HORIZONTAL, 1, 0),
                new Action(Action.Type.VERTICAL, 0, 1));

        DotsBoxesGame game = new DotsBoxesGame(2, 2, p1, p2);

        Player winner = game.play();

        assertEquals(1, winner.getId());
    }

    @Test
    void invalidMoveShouldLoseTurnAndGameStillFinishes() {
        ScriptedPlayer p1 = new ScriptedPlayer(0,
                new Action(Action.Type.HORIZONTAL, 0, 1),
                new Action(Action.Type.HORIZONTAL, 0, 0),
                new Action(Action.Type.VERTICAL, 0, 0));
        ScriptedPlayer p2 = new ScriptedPlayer(1,
                new Action(Action.Type.HORIZONTAL, 1, 0),
                new Action(Action.Type.VERTICAL, 0, 1));

        DotsBoxesGame game = new DotsBoxesGame(2, 2, p1, p2);

        Player winner = game.play();
        Referee referee = Referee.getInstance();

        assertNull(winner);
        assertEquals(0, referee.getScore(p1));
        assertEquals(0, referee.getScore(p2));
    }

    @Test
    void createPlayerShouldSupportGloutonAndExpertModes() throws Exception {
        Method method = DotsBoxesGame.class.getDeclaredMethod("createPlayer", int.class, int.class);
        method.setAccessible(true);

        Player glouton = (Player) method.invoke(null, 0, 2);
        Player expert = (Player) method.invoke(null, 1, 6);

        assertTrue(glouton instanceof AutomatePlayer);
        assertEquals("Glouton", ((AutomatePlayer) glouton).getStrategyName());
        assertTrue(expert instanceof AIPlayer);
        assertEquals("Expert", ((AIPlayer) expert).getMoveStrategyName());
    }

    @Test
    void nullActionShouldBePenalizedAsInvalidMove() {
        Player p1 = new NullThenFallbackPlayer(0);
        ScriptedPlayer p2 = new ScriptedPlayer(1,
                new Action(Action.Type.HORIZONTAL, 1, 0),
                new Action(Action.Type.VERTICAL, 0, 1));

        DotsBoxesGame game = new DotsBoxesGame(2, 2, p1, p2);
        Player winner = game.play();
        Referee referee = Referee.getInstance();

        assertNull(winner);
        assertEquals(0, referee.getScore(p1));
        assertEquals(0, referee.getScore(p2));
    }

    private static final class ScriptedPlayer implements Player {
        private final int id;
        private final Queue<Action> actions;

        private ScriptedPlayer(int id, Action... actions) {
            this.id = id;
            this.actions = new ArrayDeque<>(Arrays.asList(actions));
        }

        @Override
        public Action getAction(Board board) {
            if (!actions.isEmpty()) {
                return actions.poll();
            }
            return board.getAvailableActions().get(0);
        }

        @Override
        public int getId() {
            return id;
        }
    }

    private static final class NullThenFallbackPlayer implements Player {
        private final int id;
        private boolean first = true;

        private NullThenFallbackPlayer(int id) {
            this.id = id;
        }

        @Override
        public Action getAction(Board board) {
            if (first) {
                first = false;
                return null;
            }
            return board.getAvailableActions().get(0);
        }

        @Override
        public int getId() {
            return id;
        }
    }
}
