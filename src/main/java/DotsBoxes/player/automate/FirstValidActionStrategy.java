package DotsBoxes.player.automate;

import java.util.List;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

/*
 * parcoure le plateau et retourne le premier segment libre. 
 * Retourne null si aucun coup n'est possible.
*/
public class FirstValidActionStrategy implements ActionStrategy {

    @Override
    public Action selectAction(Board board, int playerId) {
        // on récupère les actions valides
        List<Action> actionsValides = board.getAvailableActions();
        // s'il en a pas on renvoi null
        if (actionsValides.isEmpty()) {
            return null;
        }
        // s'il en a on renvoi la première
        return actionsValides.get(0);
    }

    @Override
    public String getName() {
        return "First valid";
    }
}
