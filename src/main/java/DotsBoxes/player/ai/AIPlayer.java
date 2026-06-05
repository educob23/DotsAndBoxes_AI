package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.Player;
import DotsBoxes.player.ActionStrategy;

/**
 * Représente un joueur automatique (IA) pour le jeu Points and Cases (Dots and Boxes).
 * <p>
 * Le joueur délègue le choix de ses actions à une stratégie de sélection de coups
 * ({@link ActionStrategy}). Cette classe n'applique aucune règle elle-même :
 * la validité des actions est vérifiée par le {@link Board} et l'arbitre.
 * </p>
 */
public class AIPlayer implements Player {

    private final int id;
    private ActionStrategy moveStrategy;

    /**
     * Crée un joueur automatique avec une stratégie donnée.
     *
     * @param id identifiant du joueur (0 ou 1)
     * @param strategy stratégie utilisée pour sélectionner les actions
     */
    public AIPlayer(int id, ActionStrategy strategy) {
        this.id = id;
        this.moveStrategy = strategy;
    }

    /**
     * Modifie la stratégie utilisée par le joueur pour choisir ses actions.
     *
     * @param strategy nouvelle stratégie de sélection de coups
     */
    public void setMoveStrategy(ActionStrategy strategy) {
        this.moveStrategy = strategy;
    }

    /**
     * Retourne la stratégie actuellement utilisée par ce joueur automatique.
     *
     * @return la stratégie de sélection de coups
     */
    public ActionStrategy getMoveStrategy() {
        return moveStrategy;
    }

    /**
     * Retourne le nom de la stratégie utilisée, utile pour le débogage ou le tournoi.
     *
     * @return le nom de la stratégie
     */
    public String getMoveStrategyName() {
        return moveStrategy.getName();
    }

    /**
     * Retourne l'action que le joueur souhaite jouer.
     * <p>
     * La validité de l'action sera vérifiée par l'arbitre.
     * </p>
     *
     * @param board état courant du plateau
     * @return l'action choisie par la stratégie
     */
    @Override
    public Action getAction(Board board) {
        return moveStrategy.selectAction(board, id);
    }

    /**
     * Retourne l'identifiant du joueur.
     *
     * @return identifiant du joueur (0 ou 1)
     */
    @Override
    public int getId() {
        return id;
    }
}