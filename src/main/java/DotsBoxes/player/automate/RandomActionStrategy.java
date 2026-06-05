package DotsBoxes.player.automate;

import java.util.List;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

/*
 *  Va jouer un coup totallement aléatoire et retourner null si aucun coup n'est disponible.
*/
public class RandomActionStrategy implements ActionStrategy {

    @Override
    public Action selectAction(Board board, int playerId) {
        /* on récupère les actions valides */
        List<Action> actionsValides = board.getAvailableActions();
        /* s'il en a pas on renvoi null */
        if (actionsValides.isEmpty()) {
            return null;
        }
        /* s'il en a on renvoi une au hazard */
        int index = (int) (Math.random() * actionsValides.size());
        return actionsValides.get(index);
    }

    @Override
    public String getName() {
        return "Random";
    }
}
