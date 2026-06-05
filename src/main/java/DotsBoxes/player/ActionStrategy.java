package DotsBoxes.player;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;

/**
 * Définit une stratégie de sélection de coups pour le jeu Points et Cases
 * (Dots and Boxes).
 * <p>
 * Une stratégie encapsule l'algorithme de décision utilisé par un joueur
 * artificiel pour choisir le prochain coup à jouer, à partir de l'état
 * courant du plateau.
 * </p>
 *
 * <p>
 * Cette interface correspond au patron de conception <b>Strategy</b>.
 * Elle permet de changer dynamiquement l'algorithme de décision d'un joueur
 * sans modifier le moteur du jeu.
 * </p>
 */
public interface ActionStrategy {

    /**
     * Sélectionne l'action à jouer à partir de l'état courant du plateau.
     *
     * <p>
     * La stratégie reçoit une vue du plateau courant et doit retourner
     * une action correspondant au tracé d'un segment encore disponible.
     * </p>
     *
     * <p>
     * La validité de l'action retournée sera vérifiée par l'arbitre
     * ou par la classe {@link Board}.
     * </p>
     *
     * @param board l'état courant du plateau
     * @param playerId l'identifiant du joueur (ex. 0 ou 1)
     * @return l'action choisie, ou {@code null} s'il n'existe aucun coup possible
     */
    Action selectAction(Board board, int playerId);

    /**
     * Retourne le nom de la stratégie.
     * <p>
     * Cette information est principalement utilisée pour l'affichage,
     * le débogage ou les statistiques lors des tournois.
     * </p>
     *
     * @return le nom de la stratégie
     */
    String getName();
}