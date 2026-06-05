package DotsBoxes.player.automate;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.Player;
import DotsBoxes.player.ActionStrategy;

/**
 * Représente un joueur automatique (IA) pour le jeu Points et Cases.
 * <p>
 * Un joueur automatique délègue entièrement le choix de ses coups
 * à une implémentation de {@link ActionStrategy}.
 * </p>
 *
 * <p>
 * Cette classe correspond au patron de conception <b>Strategy</b> :
 * le comportement du joueur peut être modifié dynamiquement en
 * changeant la stratégie utilisée.
 * </p>
 */
public class AutomatePlayer implements Player {

    private final int id;
    private final ActionStrategy strategy;

    /**
     * Construit un joueur automatique.
     *
     * @param id identifiant du joueur (0 ou 1)
     * @param strategy stratégie de sélection des coups
     */
    public AutomatePlayer(int id, ActionStrategy strategy) {
        this.id = id;
        this.strategy = strategy;
    }

    /**
     * Demande à la stratégie de choisir une action à jouer.
     *
     * @param board état courant du plateau
     * @return l'action choisie par la stratégie
     */
    @Override
    public Action getAction(Board board) {
        return strategy.selectAction(board, id);
    }

    /**
     * Retourne l'identifiant du joueur.
     *
     * @return identifiant du joueur
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * Retourne le nom de la stratégie utilisée par ce joueur automatique.
     *
     * @return nom de la stratégie
     */
    public String getStrategyName() {
        return strategy.getName();
    }
}