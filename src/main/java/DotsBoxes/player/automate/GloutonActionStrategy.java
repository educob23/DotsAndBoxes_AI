package DotsBoxes.player.automate;

import java.util.List;
import java.util.Random;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

/*
 * Joue un coup si il ferme une ou plusieurs boxes ; sinon joue un coup aléatoire valide.
 * Retourne null si aucun coup est disponible.
*/
public class GloutonActionStrategy implements ActionStrategy {

    // pour pouvoir faire les choix random déterministes si on veut
    Random random;

    public GloutonActionStrategy() {
        this.random = new Random();
    }

    public GloutonActionStrategy(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public Action selectAction(Board board, int playerId) {
        // on récupère les actions valides 
        List<Action> actionsValides = board.getAvailableActions();
        // s'il en a pas on renvoi null
        if (actionsValides.isEmpty()) {
            return null;
        }
        // on regarde si les actions possibles ferment des cases et combien elles en ferment
        Action bestAction = null;
        int bestClosed = 0;  //le nb de cases que ferme bestAction

        for (Action action : actionsValides) {
            Board copy = new Board(board);               //pour éviter de modifier le vrai plateau 
            int closed = copy.apply(action, playerId);   //le nb de cases que ferme l'action

            if (closed > bestClosed) {
                bestClosed = closed;
                bestAction = action;
                if (bestClosed == 2) break; // on optimise en arrétant la boucle si on ferme 2 car c'est le max à fermer en unn coup
            }
        }

        // si aucune action ferme de cases on prend une action aléatoire
        if (bestAction == null) {
            bestAction = actionsValides.get(random.nextInt(actionsValides.size()));
        }

        return bestAction;  
    }

    @Override
    public String getName() {
        return "Glouton";
    }
}
